package reika.dragonapi.instantiable.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * V33a {@code NBTFile}: a file (or, with a reference class, a classpath resource) holding a header compound, optional
 * extra data, and a list of data compounds, optionally gzip-compressed and optionally "encrypted" (its bytes reversed).
 * Subclasses define what goes in each part; {@link SimpleNBTFile} holds one compound.
 */
public abstract class NBTFile {

	public final String name;

	private final String filepath;
	private final Class reference;

	public boolean compressData = false;
	public boolean encryptData = false;

	public NBTFile(File f) {
		this(f.getName(), f);
	}

	public NBTFile(String name, File f) {
		this(name, f.getAbsolutePath(), null);
	}

	public NBTFile(String name, String path, Class c) {
		this.name = name;
		filepath = path;
		reference = c;
	}

	private static byte[] reverse(byte[] data) {
		byte[] arr = new byte[data.length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = data[data.length-1-i];
		}
		return arr;
	}

	private CompoundTag read(InputStream in) throws IOException {
		byte[] data = in.readAllBytes();
		if (encryptData)
			data = reverse(data);
		ByteArrayInputStream bin = new ByteArrayInputStream(data);
		return compressData ? NbtIo.readCompressed(bin, NbtAccounter.unlimitedHeap()) : NbtIo.read(new DataInputStream(bin));
	}

	public final void load() throws IOException {
		if (reference != null) {
			try (InputStream in = reference.getResourceAsStream(filepath)) {
				if (in == null)
					return;
				this.setDataFromLines(this.read(in));
			}
		}
		else {
			File f = new File(filepath);
			if (!f.exists())
				return;
			try (InputStream in = new FileInputStream(f)) {
				this.setDataFromLines(this.read(in));
			}
		}
	}

	public final void save() throws IOException {
		File f = new File(filepath);
		f.getParentFile().mkdirs();
		CompoundTag tag = this.getDataAsLines();
		java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
		if (compressData)
			NbtIo.writeCompressed(tag, bout);
		else
			NbtIo.write(tag, new java.io.DataOutputStream(bout));
		byte[] data = bout.toByteArray();
		if (encryptData)
			data = reverse(data);
		try (OutputStream out = new FileOutputStream(f)) {
			out.write(data);
		}
	}

	private CompoundTag getDataAsLines() {
		CompoundTag dat = new CompoundTag();
		CompoundTag header = new CompoundTag();
		this.writeHeader(header);

		CompoundTag extra = this.writeExtraData();
		if (extra != null)
			header.put("extra", extra);

		dat.put("header", header);

		ListTag li = new ListTag();
		this.writeData(li);
		dat.put("data", li);
		return dat;
	}

	private void setDataFromLines(CompoundTag tag) {
		CompoundTag header = tag.getCompoundOrEmpty("header");
		this.readHeader(header);

		CompoundTag extra = header.getCompoundOrEmpty("extra");
		this.readExtraData(extra);

		ListTag li = tag.getListOrEmpty("data");
		this.readData(li);
	}

	protected abstract void readHeader(CompoundTag header);

	/** Is a list of CompoundTags! */
	protected abstract void readData(ListTag li);

	protected abstract void readExtraData(CompoundTag extra);

	protected abstract void writeHeader(CompoundTag header);

	/** Write a list of CompoundTags! */
	protected abstract void writeData(ListTag li);

	protected abstract CompoundTag writeExtraData();

	public static final class SimpleNBTFile extends NBTFile {

		public CompoundTag data;

		public SimpleNBTFile(File f) {
			super(f);
		}

		@Override
		protected void readHeader(CompoundTag header) {

		}

		@Override
		protected void writeHeader(CompoundTag header) {

		}

		@Override
		protected void readData(ListTag li) {
			Tag t = li.isEmpty() ? null : li.get(0);
			data = t instanceof CompoundTag tag && !tag.isEmpty() ? tag : null;
		}

		@Override
		protected void writeData(ListTag li) {
			if (data != null) {
				li.add(data.copy());
			}
		}

		@Override
		protected void readExtraData(CompoundTag extra) {

		}

		@Override
		protected CompoundTag writeExtraData() {
			return null;
		}

	}

}
