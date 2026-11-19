package zone.moddev.mc.ironagearchitecture.probe;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.GsonBuilder;

final class MPFiles {
    static void write(Path path,Object data){try{Files.createDirectories(path.getParent());Path temporary=path.resolveSibling(path.getFileName()+".tmp");Files.write(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(data).getBytes(StandardCharsets.UTF_8));Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new RuntimeException(e);}}
}
