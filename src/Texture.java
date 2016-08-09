import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public enum Texture{
    BRICK ("brick.png"),
    BLUE ("blue.png"),
    WOOD ("wood.png"),
    STONE ("stone.png"),
    MOSS ("moss.png"),
    EAGLE ("eagle.png");

    private BufferedImage image = null;

    Texture(String fileName) {
        try {
            image = ImageIO.read(new File("textures/" + fileName));
        } catch (IOException e) {
            System.out.println("Texture load failed: " + e.getMessage());
        }
    }

    public BufferedImage getImage(){
        return image;
    }
}
