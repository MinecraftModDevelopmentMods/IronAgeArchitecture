package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;

/** Pure facade grammar. Coordinates and openings determine repeatable timber bays. */
public final class SmartFacade {
    public static final int LEFT=1, RIGHT=2, CENTRE=4, BOTTOM=8, TOP=16, RISE=32, FALL=64, FACE=128;
    public static final int WALL=1, OPENING=2, POST=3, RAIL=4, RISING=5, FALLING=6, HEAD=7, CROSS=8;
    private SmartFacade() { }

    public static int[][] arrange(int[][] cells) {
        cells=withOpenings(cells);
        int height=cells.length, width=cells[0].length;
        int[][] result=new int[height][width];
        int widest=0, gable=height;
        for(int[] row:cells) {
            int count=0;for(int value:row) if(value!=0) count++;
            widest=Math.max(widest,count);
        }
        for(int y=2;y<height;y++) {
            int count=0;for(int value:cells[y]) if(value!=0) count++;
            if(count<widest) {gable=y;break;}
        }
        int base=0;
        while(base<gable) {
            int end=gable-base<=5?gable:base+4;
            storey(cells,result,base,end);
            base=end;
        }
        for(int y=gable;y<height;y++) for(int x=0;x<width;x++) if(cells[y][x]!=0) {
            int code=FACE;
            if(x==0||cells[y][x-1]==0) code|=LEFT;
            if(x==width-1||cells[y][x+1]==0) code|=RIGHT;
            if(x==width/2) code|=CENTRE;
            if(y==gable) code|=BOTTOM;
            if((y-gable)%2==1||y==height-1||cells[y+1][x]==0) code|=TOP;
            result[y][x]=code;
        }
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int kind=cells[y][x];
            if(kind==RAIL||kind==HEAD) {
                for(int column=0;column<width;column++) if(cells[y][column]==WALL)
                    result[y][column]|=kind==RAIL?BOTTOM:TOP;
            }
            if(kind==RISING||kind==CROSS) extendBrace(cells,result,x,y,true);
            if(kind==FALLING||kind==CROSS) extendBrace(cells,result,x,y,false);
        }
        return result;
    }

    private static int[][] withOpenings(int[][] source) {
        int height=source.length,width=source[0].length;
        int[][] result=new int[height][];
        for(int y=0;y<height;y++) result[y]=source[y].clone();
        boolean[][] visited=new boolean[height][width];
        for(int row=0;row<height;row++) for(int column=0;column<width;column++) {
            if(visited[row][column]||source[row][column]!=0) continue;
            List<int[]> region=new ArrayList<>();
            ArrayDeque<int[]> pending=new ArrayDeque<>();
            pending.add(new int[]{column,row});
            boolean outside=false;
            while(!pending.isEmpty()) {
                int[] at=pending.removeFirst();int x=at[0],y=at[1];
                if(x<0||x>=width||y<0||y>=height||visited[y][x]||source[y][x]!=0) continue;
                visited[y][x]=true;region.add(at);
                // Openings may meet the sill, as a doorway does, but need both jambs and a head.
                if(x==0||x==width-1||y==height-1) outside=true;
                pending.add(new int[]{x-1,y});pending.add(new int[]{x+1,y});
                pending.add(new int[]{x,y-1});pending.add(new int[]{x,y+1});
            }
            if(!outside) for(int[] at:region) result[at[1]][at[0]]=OPENING;
        }
        return result;
    }

    private static void extendBrace(int[][] cells,int[][] result,int x,int y,boolean rising) {
        for(int direction:new int[]{-1,1}) for(int step=1;step<cells.length;step++) {
            int column=x+direction*step,row=y+(rising?direction:-direction)*step;
            if(row<0||row>=cells.length||column<0||column>=cells[0].length||cells[row][column]!=WALL) break;
            int code=result[row][column];
            result[row][column]|=rising?RISE:FALL;
            int horizontalEnd=direction>0?RIGHT:LEFT;
            int verticalEnd=(rising?direction:-direction)>0?TOP:BOTTOM;
            if((code&(horizontalEnd|verticalEnd))!=0) break;
        }
    }

    private static void storey(int[][] cells,int[][] result,int base,int end) {
        int width=cells[0].length;
        SortedSet<Integer> anchors=new TreeSet<>();
        anchors.add(0);anchors.add(width);
        int signature=width*31+(end-base);
        for(int y=base;y<end;y++) for(int x=0;x<width;x++) {
            int kind=cells[y][x];
            if(kind==0) continue;
            signature=31*signature+kind*(x+1)*(y-base+1);
            if(x==0||cells[y][x-1]==0) anchors.add(x);
            if(x==width-1||cells[y][x+1]==0) anchors.add(x+1);
            if(kind==OPENING) {
                if(x==0||cells[y][x-1]!=OPENING) anchors.add(x);
                if(x==width-1||cells[y][x+1]!=OPENING) anchors.add(x+1);
            }
            if(kind==POST) {anchors.add(x);anchors.add(x+1);}
        }
        List<Integer> required=new ArrayList<>(anchors), posts=new ArrayList<>();
        for(int a=1;a<required.size();a++) {
            int start=required.get(a-1), gap=required.get(a)-start;
            int count=Math.max(1,(gap+1)/3), small=gap/count, extra=gap%count;
            posts.add(start);
            for(int i=0;i<count-1;i++) {
                int position=(signature&1)==0?i:count-i-1;
                start+=small+(position<extra?1:0);
                posts.add(start);
            }
        }
        posts.add(width);
        for(int y=base;y<end;y++) for(int x=0;x<width;x++) if(cells[y][x]!=0) {
            int code=FACE;
            if(posts.contains(x)||x==0||cells[y][x-1]==0) code|=LEFT;
            if(posts.contains(x+1)||x==width-1||cells[y][x+1]==0) code|=RIGHT;
            if(y==base||cells[y-1][x]==OPENING) code|=BOTTOM;
            if(y==end-1||cells[y+1][x]==OPENING) code|=TOP;
            if(cells[y][x]==RAIL) code|=BOTTOM;
            result[y][x]=code;
        }
        for(int i=1;i<posts.size();i++) {
            int left=posts.get(i-1), right=posts.get(i), clearHeight=end-base;
            for(int y=base;y<end;y++) for(int x=left;x<right;x++)
                if(cells[y][x]==0||cells[y][x]==OPENING) clearHeight=Math.min(clearHeight,y-base);
            int reach=Math.min(right-left,clearHeight);
            if(reach==0) continue;
            if(right-left==1&&clearHeight>1) {
                for(int y=base;y<base+clearHeight;y++) result[y][left]|=CENTRE;
                continue;
            }
            boolean rising=((signature+i)&1)==0;
            diagonal(result,left,right,base,reach,rising);
            if(right-left>=3&&clearHeight>=3&&Math.floorMod(signature+i,3)==0)
                diagonal(result,left,right,base,reach,!rising);
        }
    }

    private static void diagonal(int[][] result,int left,int right,int base,int reach,boolean rising) {
        for(int step=0;step<reach;step++) result[base+step][rising?left+step:right-step-1]|=rising?RISE:FALL;
    }
}
