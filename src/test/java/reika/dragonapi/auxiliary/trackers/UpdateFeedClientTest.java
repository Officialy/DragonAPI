package reika.dragonapi.auxiliary.trackers;

import static org.junit.jupiter.api.Assertions.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import reika.dragonapi.auxiliary.trackers.UpdateFeedClient.Status;

class UpdateFeedClientTest {
    private static UpdateFeedClient.Result parse(String body) { return UpdateFeedClient.parse(body.getBytes(StandardCharsets.UTF_8),"Dragon API"); }
    @Test void originalRowsSupportLegacyAndArtifactVersionsWithoutComparingErrorSentinels() {
        assertEquals("v42a",parse("# versions\nrotarycraft:v42b\ndragonapi:v42a\n").version().orElseThrow().toString());
        assertEquals("1.2.3-beta.4",parse("Dragon API:1.2.3-beta.4\n").version().orElseThrow().toString());
        for(String value:new String[]{"","no version","[URL TIMEOUT]","Source Code","-1"})
            assertEquals(Status.MALFORMED,parse("dragonapi:"+value).status(),value);
        assertEquals(Status.MISSING,parse("rotarycraft:v42a").status());
        assertEquals(Status.MALFORMED,parse("<html>maintenance</html>").status());
        assertEquals(Status.MALFORMED,parse("dragonapi:1.0.0\ndragonapi:2.0.0").status());
        assertEquals(Status.MALFORMED,UpdateFeedClient.parse(new byte[]{(byte)0xff},"dragonapi").status());
    }
    private static final class Endpoint implements AutoCloseable {
        final ServerSocket socket; final Thread responder;
        Endpoint(int code,String headers,byte[] body,boolean stall) throws Exception {
            socket=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
            responder=new Thread(()->{
                try(var connection=socket.accept()) {
                    connection.setSoTimeout(1000);
                    var input=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.US_ASCII));
                    for(String line; (line=input.readLine())!=null && !line.isEmpty();) {}
                    if(stall) { Thread.sleep(500); return; }
                    var out=connection.getOutputStream();
                    out.write(("HTTP/1.1 "+code+" Fixture\r\nConnection: close\r\nContent-Length: "+body.length+"\r\n"+headers+"\r\n").getBytes(StandardCharsets.US_ASCII));
                    out.write(body); out.flush();
                } catch(Exception ignored) { /* Timeouts/closed connections are intentional fixture outcomes. */ }
            },"update-feed-loopback"); responder.setDaemon(true); responder.start();
        }
        URI uri() { return URI.create("http://127.0.0.1:"+socket.getLocalPort()+"/versions_26-3.txt"); }
        public void close() throws Exception { socket.close(); responder.join(1000); }
    }
    @Test void actualTransportAcceptsMissingOrValidOptionalLastModified() throws Exception {
        byte[] body="dragonapi:1.2.0".getBytes(StandardCharsets.UTF_8);
        try(var endpoint=new Endpoint(200,"",body,false)) {
            var result=UpdateFeedClient.fetch(endpoint.uri(),"dragonapi",1000);
            assertEquals(Status.VERIFIED,result.status()); assertTrue(result.modified().isEmpty());
        }
        try(var endpoint=new Endpoint(200,"Last-Modified: Wed, 07 Oct 2026 12:00:00 GMT\r\n",body,false)) {
            var result=UpdateFeedClient.fetch(endpoint.uri(),"dragonapi",1000);
            assertEquals(Status.VERIFIED,result.status()); assertTrue(result.modified().isPresent());
        }
    }
    @Test void unavailableAndRedirectEndpointsCannotProduceAnUpdateVersion() throws Exception {
        for(int code:new int[]{404,500,302}) try(var endpoint=new Endpoint(code,"Location: http://127.0.0.1:1/other\r\n",new byte[0],false)) {
            var result=UpdateFeedClient.fetch(endpoint.uri(),"dragonapi",1000);
            assertEquals(Status.UNAVAILABLE,result.status()); assertTrue(result.version().isEmpty());
        }
    }
    @Test void stalledAndOversizedResponsesAreBounded() throws Exception {
        try(var endpoint=new Endpoint(200,"",new byte[0],true)) {
            assertEquals(Status.UNAVAILABLE,UpdateFeedClient.fetch(endpoint.uri(),"dragonapi",100).status());
        }
        try(var endpoint=new Endpoint(200,"",new byte[UpdateFeedClient.MAX_BYTES+1],false)) {
            assertEquals(Status.MALFORMED,UpdateFeedClient.fetch(endpoint.uri(),"dragonapi",1000).status());
        }
        assertEquals(Status.MALFORMED,UpdateFeedClient.fetch(URI.create("file:///versions.txt"),"dragonapi").status());
    }
}
