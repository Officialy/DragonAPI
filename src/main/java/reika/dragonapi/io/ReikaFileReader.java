/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.io;

import com.google.common.base.Throwables;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaStringParser;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import org.jspecify.annotations.Nullable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarFile;

import static net.neoforged.fml.loading.FMLEnvironment.isProduction;

public class ReikaFileReader {

	private static long internetLastUnavailable = -1;

	public static int getFileLength(File f) {
		try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
			int len = 0;
			while (reader.readLine() != null)
				len++;
			return len;
		} catch (Exception e) {
			throw new RuntimeException("Could not load file data from " + f, e);
		}
	}

    /** Caller owns the returned reader; failures are logged and return null. */
    public static @Nullable BufferedReader getReader(@Nullable File file, Charset charset) {
        try {
            return openLocalReader(Objects.requireNonNull(file, "file").toPath(), charset);
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not open text file {} with charset {}", file, charset, error);
            return null;
        }
    }

    private static BufferedReader openLocalReader(Path file, Charset charset) throws IOException {
        // Preserve InputStreamReader's historical replacement policy, while constructing the
        // decoder before opening the file so invalid charsets cannot leak an open handle.
        var decoder = Objects.requireNonNull(charset, "charset").newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder));
    }

    /** Successful wrapping transfers stream ownership to the reader; failures leave it with the caller. */
    public static @Nullable BufferedReader getReader(@Nullable InputStream input, Charset charset) {
        try {
            return new BufferedReader(new InputStreamReader(input, charset));
        } catch (RuntimeException error) {
            DragonAPI.LOGGER.error("Could not wrap text stream with charset {}", charset, error);
            return null;
        }
    }

    /** Caller owns the returned reader; failures are logged and return null. */
    public static @Nullable BufferedReader getReader(@Nullable String path, Charset charset) {
        try {
            return openLocalReader(Path.of(path), charset);
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not open text path {} with charset {}", path, charset, error);
            return null;
        }
    }

	/**
	 * Make sure you close this!
	 */
	public static BufferedReader getReader(URL url, int timeout, ConnectionErrorHandler ch, DataFetcher f) {
		if (!isInternetAccessible(timeout)) {
			if (ch != null)
				ch.onNoInternet();
			return null;
		}

		try {
			URLConnection c = url.openConnection();
			c.setConnectTimeout(timeout);
			if (f != null) {
				try {
					f.fetchData(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return new BufferedReader(new InputStreamReader(c.getInputStream()));
		} catch (UnknownHostException e) { //Server not found
			if (ch != null)
				ch.onServerNotFound();
		} catch (ConnectException e) { //Redirect/tampering
			if (ch != null)
				ch.onServerRedirected();
		} catch (SocketTimeoutException e) { //Slow internet, cannot load a text file...
			if (ch != null)
				ch.onTimedOut();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean isInternetAccessible(int timeout) {
		/*if (ReikaObfuscationHelper.isDeObfEnvironment() /*&& ReikaJVMParser.isArgumentPresent("-DragonAPI_NoInternet")) {
			DragonAPI.LOGGER.info("Internet is marked inaccessible.");
			return false;
		}*/
		int dt = 60 * 1000 * (isProduction() ? 5 : 1);
		if (internetLastUnavailable + dt >= System.currentTimeMillis()) //only check at most once a minute (five for dev)
			return false;
		String[] attempts = {
				"http://www.google.com",
				"http://en.wikipedia.org/wiki/Main_Page",
				"http://github.com/",
				"http://msdn.microsoft.com/en-us/default.aspx",
				"https://aws.amazon.com/",
				"ns1.telstra.net"
		};
		for (int i = 0; i < attempts.length; i++) {
			HttpURLConnection c = null;
			try {
				URLConnection connection = new URL(attempts[i]).openConnection();
				if (!(connection instanceof HttpURLConnection))
					continue;
				c = (HttpURLConnection)connection;
				c.setConnectTimeout(timeout);
				c.setReadTimeout(timeout);
				c.getResponseCode();
				return true;
			} catch (IOException ex) {
				// Try the next independent endpoint.
			} finally {
				if (c != null)
					c.disconnect();
			}
		}
		internetLastUnavailable = System.currentTimeMillis();
		return false;
	}

    /**
     * Recursively lists files. No extensions (or a null filter) include every file.
     * Canonical directory identities prevent links/aliases from cycling indefinitely.
     */
    public static ArrayList<File> getAllFilesInFolder(File directory, String... extensions) {
        Objects.requireNonNull(directory, "directory");
        var result = new ArrayList<File>();
        collectFiles(directory, extensions == null ? null : extensions.clone(), new HashSet<>(), result);
        return result;
    }

    private static void collectFiles(File directory, String @Nullable [] extensions, Set<Path> visited, ArrayList<File> result) {
        if (!directory.isDirectory()) return;
        try {
            if (!visited.add(directory.toPath().toRealPath())) return;
        } catch (IOException | SecurityException error) {
            DragonAPI.LOGGER.warn("Could not resolve directory {} while collecting files", directory, error);
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collectFiles(child, extensions, visited, result);
            else if (extensions == null || extensions.length == 0 || Arrays.stream(extensions)
                    .anyMatch(extension -> extension == null || child.getName().endsWith(extension))) result.add(child);
        }
    }

    public static ArrayList<File> getAllFilesInFolder(File directory) {
        return getAllFilesInFolder(directory, (String[])null);
    }

	public static String readTextFile(Class<?> root, String path) throws IOException {
		try(InputStream in = root.getResourceAsStream(path)) {
			if (in == null) {
				DragonAPI.LOGGER.error("File "+path+" does not exist!");
				return "";
			}
			StringBuilder sb = new StringBuilder();
			BufferedReader p;
			try {
				p = new BufferedReader(new InputStreamReader(in));
			}
			catch (NullPointerException e) {
				return sb.toString();
			}
			int i = 0;
			try {
				String line = null;
				while((line = p.readLine()) != null) {
					if (!line.isEmpty()) {
						sb.append(line);
						i++;
						sb.append("\n");
					}
				}
				p.close();
			}
			catch (Exception e) {
				DragonAPI.LOGGER.error("Could not read resource {} relative to {} near nonempty line {}", path, root.getName(), i, e);
			}
			return sb.toString();
		}
	}

	public static ArrayList<String> getFileAsLines(String path, boolean printStackTrace, Charset set) {
		return getFileAsLines(getReader(path, set), printStackTrace);
	}

	public static ArrayList<String> getFileAsLines(URL url, int timeout, boolean printStackTrace, ConnectionErrorHandler ch) {
		return getFileAsLines(url, timeout, printStackTrace, ch, null);
	}

	public static ArrayList<String> getFileAsLines(URL url, int timeout, boolean printStackTrace, ConnectionErrorHandler ch, DataFetcher f) {
		BufferedReader r = getReader(url, timeout, ch, f);
		return r != null ? getFileAsLines(r, printStackTrace) : null;
	}

	@Deprecated
	public static ArrayList<String> getFileAsLines(File f, boolean printStackTrace) {
		return getFileAsLines(f, printStackTrace, Charset.defaultCharset());
	}

	public static ArrayList<String> getFileAsLines(File f, boolean printStackTrace, Charset set) {
		return getFileAsLines(getReader(f, set), printStackTrace);
	}

	@Deprecated
	public static ArrayList<String> getFileAsLines(InputStream in, boolean printStackTrace) {
		return getFileAsLines(in, printStackTrace, Charset.defaultCharset());
	}

	public static ArrayList<String> getFileAsLines(InputStream in, boolean printStackTrace, Charset set) {
		return getFileAsLines(getReader(in, set), printStackTrace);
	}

	public static ArrayList<String> getFileAsLines(@Nullable BufferedReader r, boolean printStackTrace) {
		ArrayList<String> li = new ArrayList<>();
		if (r == null)
			return li;
		String line = "";
		try {
			while (line != null) {
				line = r.readLine();
				if (line != null) {
					li.add(line);
				}
			}
		} catch (Exception e) {
			if (printStackTrace)
				DragonAPI.LOGGER.error("Could not read text lines", e);
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				DragonAPI.LOGGER.error("Could not close text reader", e);
			}
		}
		return li;
	}

	public static ArrayList<Byte> getFileAsBytes(InputStream in, boolean printStackTrace, Charset set) {
		ArrayList<Byte> li = new ArrayList<>();
		try (InputStream source = in) {
			int value;
			while ((value = source.read()) != -1)
				li.add((byte)value);
		} catch (Exception e) {
			if (printStackTrace)
				e.printStackTrace();
		}
		return li;
	}

    /** Caller owns the writer; the default overload preserves the platform charset. */
    public static @Nullable BufferedWriter getWriter(File file) {
        return getWriter(file, Charset.defaultCharset());
    }

    public static @Nullable BufferedWriter getWriter(File file, Charset charset) {
        try {
            return openLocalWriter(file.toPath(), charset);
        } catch (IOException | RuntimeException error) {
            DragonAPI.LOGGER.error("Could not open text output {} with charset {}", file, charset, error);
            return null;
        }
    }

    private static BufferedWriter openLocalWriter(Path file, Charset charset) throws IOException {
        var encoder = Objects.requireNonNull(charset, "charset").newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file), encoder));
    }

    public static void writeLinesToFile(String path, ArrayList<String> lines, boolean printStackTrace) {
        writeLinesToFile(new File(path), lines, printStackTrace);
    }

    public static void writeLinesToFile(File file, ArrayList<String> lines, boolean printStackTrace) {
        writeLinesToFile(file, lines, printStackTrace, Charset.defaultCharset());
    }

    /** Explicit charset for callers which own a portable text format. */
    public static void writeLinesToFile(File file, ArrayList<String> lines, boolean printStackTrace, Charset charset) {
        try {
            writeLinesAndClose(openLocalWriter(file.toPath(), charset), lines);
        } catch (IOException error) {
            if (printStackTrace) DragonAPI.LOGGER.error("Could not write text lines to {} with charset {}", file, charset, error);
        }
    }

    /** Owns/closes the supplied writer, including when writing or flushing fails. */
    public static void writeLinesToFile(BufferedWriter writer, ArrayList<String> lines, boolean printStackTrace) {
        try {
            writeLinesAndClose(writer, lines);
        } catch (IOException error) {
            if (printStackTrace) DragonAPI.LOGGER.error("Could not write text lines to supplied writer", error);
        }
    }

    private static void writeLinesAndClose(BufferedWriter writer, Collection<String> lines) throws IOException {
        String separator = System.getProperty("line.separator");
        try (writer) {
            for (String line : lines) {
                writer.write(String.valueOf(line));
                writer.write(separator);
            }
        }
    }

    /** Replaces complete binary output without deleting existing data before the new bytes are ready. */
    public static void writeDataToFile(File file, ArrayList<Byte> values, boolean printStackTrace) {
        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = values.get(i);
        Path destination = file.toPath().toAbsolutePath();
        Path temporary = null;
        try {
            Path parent = destination.getParent();
            if (parent == null || Files.isDirectory(destination))
                throw new IOException("Binary output is not a regular file path: " + destination);
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "reika-data-", ".tmp");
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException error) {
            if (printStackTrace) DragonAPI.LOGGER.error("Could not replace binary output {}", destination, error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException error) {
                    if (printStackTrace) DragonAPI.LOGGER.error("Could not remove incomplete binary output {}", temporary, error);
                }
            }
        }
    }

    public static InputStream convertLinesToStream(ArrayList<String> lines, boolean printStackTrace, Charset charset) {
        var bytes = new ByteArrayOutputStream();
        try {
            writeLinesAndClose(new BufferedWriter(new OutputStreamWriter(bytes, charset)), lines);
        } catch (IOException error) {
            if (printStackTrace) DragonAPI.LOGGER.error("Could not encode text lines with charset {}", charset, error);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

	public static String getHash(String path, HashType type) {
		return getHash(new File(path), type);
	}

	public static String getHash(File file, HashType type) {
		try (FileInputStream in = new FileInputStream(file)) {
			return getHash(in, type);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String getHash(InputStream is, HashType type) {
		try {
			byte[] buffer = new byte[1024];
			MessageDigest complete = MessageDigest.getInstance(type.tag);
			int numRead;

			do {
				numRead = is.read(buffer);
				if (numRead > 0)
					complete.update(buffer, 0, numRead);
			}
			while (numRead != -1);

			byte[] hash = complete.digest();
			return HexFormat.of().withUpperCase().formatHex(hash);
		} catch (Exception e) {
			throw new IllegalStateException("Could not calculate " + type + " hash", e);
		}
	}

	public static InputStream getFileInsideJar(File f, String name) {
		try {
			JarFile jar = new JarFile(f);
            InputStream entry = getFileInsideJar(jar, name);
            if (entry == null) {
                jar.close();
                return null;
            }
            return new FilterInputStream(entry) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { jar.close(); }
                }
            };
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static InputStream getFileInsideJar(JarFile jar, String name) {
		try {
			var entry = jar.getEntry(name);
            return entry != null ? jar.getInputStream(entry) : null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean deleteFolderWithContents(File f) {
		return deleteFolderWithContents(f, 10);
	}

	public static boolean deleteFolderWithContents(File f, int tries) {
		Exception e = null;
		for (int i = 0; i < tries; i++) {
			try {
				FileUtils.forceDelete(f);
				return true;
			} catch (Exception ex) {
				e = ex;
			}
		}
		if (e != null) {
			e.printStackTrace();
		}
		return false;
	}

	public static void copyFile(File in, File out, int size) throws FileReadException, FileWriteException, FileNotFoundException {
		try (InputStream input = new FileInputStream(in)) {
            copyFile(input, new FileOutputStream(out), size, null);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new FileReadException(e);
        }
	}

	/**
	 * Closes the streams for you.
	 */
	public static void copyFile(InputStream in, OutputStream out, int size) throws FileReadException, FileWriteException {
		copyFile(in, out, size, null);
	}

	/**
	 * Closes the streams for you.
	 */
	public static void copyFile(InputStream in, OutputStream out, int chunkSize, WriteCallback call) throws FileReadException, FileWriteException {
		try {
			byte[] bytes = new byte[chunkSize];
			int count = 0;
			while (count != -1) {
				if (count > 0) {
					try {
						out.write(bytes, 0, count);
						if (call != null)
							call.onWrite(bytes);
					} catch (IOException e) {
						throw new FileWriteException(e);
					}
				}
				try {
					count = in.read(bytes, 0, bytes.length);
				} catch (IOException e) {
					throw new FileReadException(e);
				}
			}
		} catch (Exception e) {
			throw Throwables.propagate(e);
		} finally {
			try {
				try { in.close(); } finally { out.close(); }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static File createFileFromStream(InputStream in) throws IOException {
		File tempFile = File.createTempFile("temp_" + in.hashCode(), null);
		tempFile.deleteOnExit();
		try (InputStream source = in; FileOutputStream out = new FileOutputStream(tempFile)) {
			IOUtils.copy(source, out);
		}
		return tempFile;
	}

	public static boolean isEmpty(File f) throws IOException {
		try (BufferedReader br = getReader(f, Charset.defaultCharset())) {
			String line = br.readLine();
			return line == null || (line.length() == 0 && br.readLine() == null);
		}
	}

	public static void emptyDirectory(File dir) {
		File[] f = dir.listFiles();
		if (f == null)
			return;
		for (int i = 0; i < f.length; i++) {
			f[i].delete();
		}
	}

	public static void clearFile(File f) {
		try {
			f.delete();
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void flipFileBytes(InputStream in, File out) {
		try (InputStream source = in) {
			ArrayList<Byte> li = new ArrayList<>();
			int value;
			while ((value = source.read()) != -1)
				li.add((byte)value);
			Collections.reverse(li);
			writeDataToFile(out, li, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static ArrayList<String> encryptFileBytes(InputStream in) {
		try (InputStream source = in) {
			ArrayList<Byte> li = new ArrayList<>();
			int value;
			while ((value = source.read()) != -1)
				li.add((byte)value);
			encryptByteList(li);
			ArrayList<String> li2 = new ArrayList<>();
			String line = "";
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < li.size(); i++) {
				byte b2 = li.get(i);
				int val = Math.abs(b2);
				String s = String.valueOf(val);
				if (val < 10)
					s = "0" + s;
				if (val < 100)
					s = "0" + s;
				if (b2 < 0)
					s = "-" + s;
				else
					s = "0" + s;
				sb.append(s);
				if (i % 56 == 55 || i == li.size() - 1) {
					li2.add(sb.toString());
					sb = new StringBuilder();
				}
			}
			return li2;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void encryptByteList(ArrayList<Byte> li) {
		Collections.reverse(li);
		for (int i = li.size() - 1; i >= 0; i--) {
			byte get = li.get(i);
			get = ReikaJavaLibrary.flipBits(get);
			get = (byte) (~get);
			li.set(i, get);
			li.add(i + 1, (byte) DragonAPI.rand.nextInt(255));
		}
	}

	public static ArrayList<Byte> decryptFileBytes(InputStream in, Charset set) {
		return decryptByteList(getFileAsLines(in, true, set));
	}

	public static ArrayList<Byte> decryptByteList(ArrayList<String> li2) {
		ArrayList<Byte> li = new ArrayList<>();
		for (String s : li2) {
			List<String> arr = ReikaStringParser.splitStringByLength(s, 4);
			for (String s2 : arr) {
				byte b = Byte.parseByte(s2);
				li.add(b);
			}
		}
		Iterator<Byte> it = li.iterator();
		int idx = 0;
		while (it.hasNext()) {
			byte b2 = it.next();
			if (idx % 2 == 1)
				it.remove();
			idx++;
		}
		for (int i = 0; i < li.size(); i++) {
			byte get = li.get(i);
			get = (byte) (~get);
			get = ReikaJavaLibrary.flipBits(get);
			li.set(i, get);
		}
		Collections.reverse(li);
		return li;
	}

	public static InputStream decryptInputStream(InputStream in, Charset set) {
		ArrayList<Byte> data = decryptFileBytes(in, set);
		ByteArrayOutputStream bin = new ByteArrayOutputStream();
		for (byte b : data)
			bin.write(b);
		return new ByteArrayInputStream(bin.toByteArray());
	}

	public static JsonElement readJSON(File f) {
		try (BufferedReader r = getReader(f, Charsets.UTF_8)) {
			return new JsonParser().parse(r);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static File getFileByNameAnyExt(File folder, String name) {
		File[] files = folder != null ? folder.listFiles() : null;
		if (files == null)
			return null;
		for (File f : files) {
			if (f.isDirectory())
				continue;
			if (getFileNameNoExtension(f, false, false).equals(name))
				return f;
		}
		return null;
	}

	public static String getFileNameNoExtension(File f, boolean full, boolean real) {
		String n = full ? (real ? getRealPath(f) : f.getAbsolutePath()) : f.getName();
		int idx = n.lastIndexOf('.');
		return idx >= 0 && idx < n.length() ? n.substring(0, idx) : n;
	}

	public static String getRelativePath(File from, File to) {
		return Paths.get(from.toURI()).relativize(Paths.get(to.toURI())).toString();
	}

	public static String getRealPath(File f) {
		try {
			return f.exists() ? f.toPath().toRealPath().toString() : f.getCanonicalPath();
		} catch (Exception e) {
			e.printStackTrace();
			return f.getAbsolutePath();
		}
	}

	public static boolean isFileWithin(File f, File dir) throws IOException {
		return f.getCanonicalPath().startsWith(dir.getCanonicalPath());
	}

	public enum HashType {
		MD5("MD5"),
		SHA1("SHA-1"),
		SHA256("SHA-256");

		private final String tag;

		HashType(String s) {
			tag = s;
		}

		public String hash(Object o) {
			return this.hashBytes(this.getBytes(o));
		}

		private String hashBytes(byte[] bytes) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(tag);
				messageDigest.update(bytes);
				// 1.7.10 used javax.xml.bind.DatatypeConverter.printHexBinary, which left the JDK in 11.
				// HexFormat.withUpperCase() is its exact replacement; the case matters because callers
				// such as ChromatiCraft's structure password take String.hashCode() of this value.
				return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
			} catch (NoSuchAlgorithmException e) {
				return null; //never happens
			}
		}

		private byte[] getBytes(Object o) {
			if (o == null) {
				DragonAPI.LOGGER.error("Cannot serialize a null object!");
				return new byte[0];
			}
			if (o instanceof byte[])
				return (byte[]) o;
			else if (o instanceof Integer)
				return ReikaJavaLibrary.splitIntToHexChars((int) o);
			else if (o instanceof Long) {
				int[] split = ReikaJavaLibrary.splitLong((long) o);
				byte[] res = new byte[8];
				byte[] low = ReikaJavaLibrary.splitIntToHexChars(split[0]);
				byte[] high = ReikaJavaLibrary.splitIntToHexChars(split[1]);
				System.arraycopy(low, 0, res, 0, low.length);
				System.arraycopy(high, 0, res, 4, high.length);
				return res;
			} else if (o instanceof String)
				return ((String) o).getBytes();
			else if (o instanceof Serializable) {
				try {
					ByteArrayOutputStream buf = new ByteArrayOutputStream();
					ObjectOutputStream oo = new ObjectOutputStream(buf);
					oo.writeObject(o);
					return buf.toByteArray();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			DragonAPI.LOGGER.error("Cannot serialize an object " + o + " of type " + o.getClass() + "!");
			return new byte[0]; //unserializable
		}
	}

	public interface ConnectionErrorHandler {

		void onServerRedirected();

		void onTimedOut();

		void onNoInternet();

		void onServerNotFound();

	}

	public interface DataFetcher {

		void fetchData(URLConnection c) throws Exception;

	}

	public interface WriteCallback {

		void onWrite(byte[] data);

	}

	/**
	 * Edits individual lines matching in a file if they match a given criterion.
	 */
	public static abstract class LineEditor {

		private final HashMap<Integer, String> lines = new HashMap();

		/**
		 * Attempt line editing?
		 */
		public abstract boolean editLine(String s, int idx);

		/**
		 * The line used to replace strings that match the criteria. Args: Original line, newline separator, line index
		 */
		protected abstract String getReplacementLine(String s, String newline, int idx);

		public final boolean performChanges(File f, Charset set) {
			try (BufferedReader r = ReikaFileReader.getReader(f, set)) {
				String sep = System.getProperty("line.separator");
				String line = r.readLine();
				StringBuilder out = new StringBuilder();
				int idx = 1;
				while (line != null) {
					lines.put(idx, line);
					String rep = this.editLine(line, idx) ? this.getReplacementLine(line, sep, idx) : line;
					if (rep == null) {

					} else {
						out.append(rep + sep);
					}
					line = r.readLine();
					idx++;
				}
				try (FileOutputStream os = new FileOutputStream(f)) {
					os.write(out.toString().getBytes(set));
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		protected final String getOriginalLine(int i) {
			return lines.get(i);
		}

	}

	public static class FileReadException extends IOException {

		private FileReadException(IOException e) {
			super(e);
		}

	}

	public static class FileWriteException extends IOException {

		private FileWriteException(IOException e) {
			super(e);
		}

	}
}

