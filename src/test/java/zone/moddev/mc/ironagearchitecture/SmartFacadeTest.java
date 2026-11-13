package zone.moddev.mc.ironagearchitecture;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import zone.moddev.mc.ironagearchitecture.block.SmartFacade;

public class SmartFacadeTest {
    @Test public void layoutsAreStableAndKeepBracesOutOfOpenings() {
        for(int width=7;width<=15;width++) for(int height=3;height<=5;height++) {
            int[][] grid=new int[height][width];
            for(int[] row:grid) Arrays.fill(row,SmartFacade.WALL);
            for(int y=1;y<height-1;y++) grid[y][2]=SmartFacade.OPENING;
            int[][] result=SmartFacade.arrange(grid);
            assertTrue(Arrays.deepEquals(result,SmartFacade.arrange(grid)));
            int braces=0;
            for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                int diagonal=result[y][x]&(SmartFacade.RISE|SmartFacade.FALL);
                if(grid[y][x]==SmartFacade.OPENING) assertEquals(0,diagonal);
                if(diagonal!=0) braces++;
            }
            assertTrue(braces>0);
            grid[1][width-2]=SmartFacade.OPENING;
            assertFalse(Arrays.deepEquals(result,SmartFacade.arrange(grid)));
        }
    }
    @Test public void incompleteAndSteppedFacadesRemainBounded() {
        int[][] grid={{1,1,1,1,1,1,1},{1,1,2,2,1,1,1},{1,1,1,1,1,1,1},
            {0,1,1,1,1,1,0},{0,0,1,1,1,0,0},{0,0,0,1,0,0,0}};
        int[][] result=SmartFacade.arrange(grid);
        for(int y=0;y<grid.length;y++) for(int x=0;x<grid[0].length;x++) {
            assertTrue(result[y][x]>=0&&result[y][x]<=255);
            if(grid[y][x]==0) assertEquals(0,result[y][x]);
        }
    }
    @Test public void unglazedOpeningsHaveTheSameFramingAsWindows() {
        int[][] empty=new int[4][9],glazed=new int[4][9];
        for(int y=0;y<4;y++){Arrays.fill(empty[y],SmartFacade.WALL);Arrays.fill(glazed[y],SmartFacade.WALL);}
        for(int y=0;y<3;y++) {empty[y][4]=0;glazed[y][4]=SmartFacade.OPENING;}
        for(int y=1;y<3;y++) {empty[y][1]=0;glazed[y][1]=SmartFacade.OPENING;}
        assertTrue(Arrays.deepEquals(SmartFacade.arrange(empty),SmartFacade.arrange(glazed)));
    }
    @Test public void manualRailsAndBracesGuideSmartPanels() {
        int[][] cells=new int[4][9];
        for(int[] row:cells) Arrays.fill(row,SmartFacade.WALL);
        cells[1][4]=SmartFacade.HEAD;
        int[][] rail=SmartFacade.arrange(cells);
        assertTrue((rail[1][1]&SmartFacade.TOP)!=0);
        cells[1][4]=SmartFacade.RISING;
        int[][] brace=SmartFacade.arrange(cells);
        assertTrue((brace[2][5]&SmartFacade.RISE)!=0);
    }
    @Test public void modeParsingDefaultsToAutomatic() {
        assertEquals(BuildingMode.AUTOMATIC,BuildingMode.parse("misspelled"));
        assertEquals(BuildingMode.AUTOMATIC,BuildingMode.parse(" Automatic "));
        assertTrue(BuildingMode.HYBRID.manual());
        assertTrue(BuildingMode.HYBRID.automatic());
    }
}
