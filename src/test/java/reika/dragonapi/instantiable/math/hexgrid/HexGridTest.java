package reika.dragonapi.instantiable.math.hexgrid;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import reika.dragonapi.instantiable.math.hexgrid.HexGrid.Hex;

class HexGridTest {
    @Test void mixedEndpointSizesDoNotChangeCubeLineOrExplicitOutputSize() {
        var from=new Hex(2,0,0,0);var to=new Hex(200,3,-3,0);
        var line=HexGrid.lineDraw(17,from,to);
        assertEquals(List.of(new Hex(17,0,0,0),new Hex(17,1,-1,0),new Hex(17,2,-2,0),new Hex(17,3,-3,0)),line);
        assertTrue(line.stream().allMatch(hex -> hex.size()==17));
        assertEquals(line,HexGrid.lineDraw(17,new Hex(99,0,0,0),new Hex(1,3,-3,0)));
    }
    @Test void reverseAndZeroLengthPathsRetainEndpointsWithoutExtraCells() {
        var from=new Hex(2,-2,0,2);var to=new Hex(5,1,0,-1);
        var line=HexGrid.lineDraw(7,from,to);var reverse=HexGrid.lineDraw(7,to,from);
        assertEquals(line,reverse.reversed());assertEquals(4,line.size());
        assertEquals(List.of(new Hex(9,-2,0,2)),HexGrid.lineDraw(9,from,from));
    }
    @Test void cubeLineStepsAreAdjacentAndKeepCubeConstraint() {
        var line=HexGrid.lineDraw(4,new Hex(1,-4,1,3),new Hex(30,3,-2,-1));
        assertEquals(8,line.size());
        for(int i=0;i<line.size();i++) {
            var current=line.get(i);assertEquals(0,current.q()+current.r()+current.s());
            if(i>0) assertEquals(1,Hex.distance(4,line.get(i-1),current));
        }
    }
}
