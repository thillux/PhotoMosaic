package de.thillux;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFile {
    private File _file;

    public ImageFile(File f) {
        _file = f;
    }

    public String getFilename() {
        return _file.getName();
    }

    public BufferedImage getImage() {
        BufferedImage img = null;
        try {
            img = ImageIO.read(_file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }
}
