import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Timer;

public class RayPanel extends JPanel implements KeyListener, Runnable {
    public boolean isRunning = false;
    public double posX = 5, posY = 5;
    public double dirX = -1, dirY = 0;
    double planeX = 0, planeY = 0.66;
    public int width, height;
    public int[][] worldMap = getWorldMap();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private int framesRendered;
    private int lastFPS;

    public RayPanel(int width, int height) {
        this.width = width;
        this.height = height;

        TimerTask updateFPS = new TimerTask() {
            public void run() {
                lastFPS = framesRendered;
                framesRendered = 0;
            }
        };

        new Timer().scheduleAtFixedRate(updateFPS, 1000, 1000);
    }

    @Override
    public void run() {
        while(isRunning){
            if(pressedKeys.size() > 0){
                try {
                    pressedKeys.forEach(this::movePlayer);
                }catch(ConcurrentModificationException ex){
                    //just move on and act like nothing happened
                }
            }
            this.repaint();
        }
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        for(int x = 0; x < width; x++) {
            //calculate ray position and direction
            double cameraX = 2 * x / (double) width - 1;
            double rayPosX = posX;
            double rayPosY = posY;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX; //Get hard int positions in the world
            int mapY = (int) posY; //For simplicity

            double sideDistX;
            double sideDistY;
            double deltaDistX = Math.sqrt(1 + (rayDirY * rayDirY) / (rayDirX * rayDirX));
            double deltaDistY = Math.sqrt(1 + (rayDirX * rayDirX) / (rayDirY * rayDirY));
            double perpWallDist;

            int stepX;
            int stepY;

            boolean hit = false; //true if the current ray hits a wall
            boolean side = false; //true if the hit wall faces south or west

            //calculate step and initial sideDist
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (rayPosX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - rayPosX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (rayPosY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - rayPosY) * deltaDistY;
            }

            while (!hit) {
                //iterate forwards until we find the wall being looked at
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = false;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = true;
                }
                //once it hits a wall, escape
                if (worldMap[mapX][mapY] > 0) {
                    hit = true;
                }
            }

            //calculate the distance of the wall from us
            if (!side) {
                perpWallDist = (mapX - rayPosX + (1 - stepX) / 2) / rayDirX;
            } else {
                perpWallDist = (mapY - rayPosY + (1 - stepY) / 2) / rayDirY;
            }

            //calculate height of this line based on distance from us
            int lineHeight = (int) (height / perpWallDist);

            //calculate lowest and highest pixel to fill in current stripe
            int drawStart = -lineHeight / 2 + height / 2;

            int drawEnd = lineHeight / 2 + height / 2;
            if (drawEnd >= height) {
                drawEnd = height - 1;
            }

            //choose wall color (based on map value)
            /*Color colour = null;
            switch (worldMap[mapX][mapY]) {
                case 1: colour = Color.RED; break;
                case 2: colour = Color.YELLOW; break;
                case 3: colour = Color.GREEN; break;
                case 4: colour = Color.ORANGE; break;
                default: colour = Color.BLACK; break;
            }
            */

            BufferedImage img;
            switch(worldMap[mapX][mapY]){
                case 1:  img = Texture.BRICK.getImage();  break; //red
                case 2:  img = Texture.MOSS.getImage();  break; //green
                case 3:  img = Texture.STONE.getImage();   break; //blue
                case 4:  img = Texture.WOOD.getImage();  break; //white
                default: img = Texture.EAGLE.getImage(); break; //yellow
            }

            double wallX; //get the x of wall for the current ray, so we can grab the right pixels from the texture
            if (side){
                wallX = rayPosX + perpWallDist * rayDirX;
            }else{
                wallX = rayPosY + perpWallDist * rayDirY;
            }
            wallX -= Math.floor(wallX);

            int imageX = (int) Math.floor(wallX * img.getWidth());
            double linesPerPixel = (double) lineHeight / img.getHeight();

            for(int i = 0; i < img.getHeight(); i++) {
                try {
                    Color colour = new Color(img.getRGB(imageX, i));
                    if (side) {
                        colour = colour.darker();
                    }
                    g.setColor(colour);
                    g.drawLine(x, (int) (drawStart + (i*linesPerPixel)), x, (int) (drawStart + (i*linesPerPixel) + linesPerPixel));
                } catch (ArrayIndexOutOfBoundsException ex) {
                    //oh well
                }
            }

            double floorX, floorY;
            BufferedImage floorTexture = Texture.STONE.getImage();
            BufferedImage ceilingTexture = Texture.BLUE.getImage();

            //decide which way we're drawing the floor tile based on the wall above it
            if(!side && rayDirX > 0){
                floorX = mapX;
                floorY = mapY + wallX;
            }else if(!side && rayDirX < 0){
                floorX = mapX + 1;
                floorY = mapY + wallX;
            }else if(side && rayDirY > 0){
                floorX = mapX + wallX;
                floorY = mapY;
            }else{
                floorX = mapX + wallX;
                floorY = mapY + 1;
            }

            double distWall = perpWallDist;
            double distPlayer = 0;

            //floor only needs to be drawn from the bottom of the wall to the bottom of the screen
            for(int y = drawEnd + 1; y < height; y++){
                //figure out which pixel of this floor stripe to use at this y value
                double currentDist = height / (2.0 * y - height);
                double weight = (currentDist - distPlayer) / (distWall - distPlayer);
                double currentFloorX = weight * floorX + (1 - weight) * posX;
                double currentFloorY = weight * floorY + (1 - weight) * posY;

                //find the image pixel for the pixel of floor we're trying to draw
                imageX = (int) ((currentFloorX * floorTexture.getWidth()) % floorTexture.getWidth());
                int imageY = (int) ((currentFloorY * floorTexture.getHeight()) % floorTexture.getHeight());

                try {
                    Color colour = new Color(floorTexture.getRGB(imageX, imageY)).darker();
                    g.setColor(colour);
                    g.drawLine(x, y, x, y);

                    colour = new Color(ceilingTexture.getRGB(imageX, imageY));
                    g.setColor(colour);
                    g.drawLine(x, height - y, x, height - y);
                }catch(IndexOutOfBoundsException ex){
                    //oh well
                }
            }
        }





        //Mini map
        int miniMapWidth = worldMap.length;
        int miniMapHeight = worldMap[0].length;
        int scale = 5;

        for(int x = 0; x < miniMapWidth; x++){
            for(int y = 0; y < miniMapHeight; y++){
                int square = worldMap[x][y];
                Color colour;
                switch(square){
                    case 0:  colour = new Color(100, 100, 100, 100); break;
                    case 1:  colour = Color.RED;  break; //red
                    case 2:  colour = Color.YELLOW;  break; //green
                    case 3:  colour = Color.GREEN;   break; //blue
                    case 4:  colour = Color.ORANGE;  break; //white
                    default: colour = Color.BLACK; break;
                }

                g.setColor(colour);
                g.fillRect(width - ((x + 1) * scale),  height - (miniMapHeight * scale) + (y * scale), scale,  scale);

                g.setColor(Color.DARK_GRAY);
                g.drawRect(width - ((x + 1) * scale), height - (miniMapHeight * scale) + (y * scale), width - (miniMapWidth * scale) + x + scale, height - (miniMapHeight* scale) + y + scale);
            }
        }

        g.setColor(Color.MAGENTA);
        g.fillOval(width - ((int) (posX + 1)*scale),  height - (miniMapHeight*scale) + ((int) posY*scale), scale, scale);
        g.setColor(Color.DARK_GRAY);
        g.drawLine(width - ((int) (posX + 1)*scale) + 2,  height - (miniMapHeight*scale) + ((int) posY*scale) + 2, (int) ((width - ((int) (posX + 1)*scale)) - dirX * 30), (int) ((height - (miniMapHeight*5) + ((int) posY*scale) + dirY * 30)));

        //FPS
        g.setColor(Color.WHITE);
        g.drawString(lastFPS + "fps", 0, 10);


        framesRendered++;
    }

    public void movePlayer(int keyCode){
        //The movement speeds are this small because we check the current pressed Keys every single time the game loop runs
        //Which processes the key lots of times for even a quick press
        double moveSpeed = 0.0000006; //the constant value is in squares/second
        double rotSpeed = 0.0000004;  //the constant value is in radians/second
        double negativeRotSpeed = rotSpeed * -1.1;
        double oldDirX = dirX, oldPlaneX = planeX;
        double xOffset = 0.2, yOffset = 0.2;

        if(dirX < 0){
            xOffset = xOffset*-1;
        }
        if(dirY < 0){
            yOffset = yOffset*-1;
        }

        switch(keyCode)
        {
            case KeyEvent.VK_UP:
                if(worldMap[(int) (posX + dirX * moveSpeed + xOffset)][(int) posY] == 0){
                    posX += dirX * moveSpeed;
                }
                if(worldMap[(int) posX][(int) (posY + dirY * moveSpeed + yOffset)] == 0){
                    posY += dirY * moveSpeed;
                }
                break;
            case KeyEvent.VK_DOWN:
                if(worldMap[(int) (posX - dirX * moveSpeed - xOffset)][(int) posY] == 0){
                    posX -= dirX * moveSpeed;
                }
                if(worldMap[(int) posX][(int) (posY - dirY * moveSpeed - yOffset)] == 0){
                    posY -= dirY * moveSpeed;
                }
                break;
            case KeyEvent.VK_RIGHT:
                dirX = dirX * Math.cos(negativeRotSpeed) - dirY * Math.sin(negativeRotSpeed);
                dirY = oldDirX * Math.sin(negativeRotSpeed) + dirY * Math.cos(negativeRotSpeed);
                planeX = planeX * Math.cos(negativeRotSpeed) - planeY * Math.sin(negativeRotSpeed);
                planeY = oldPlaneX * Math.sin(negativeRotSpeed) + planeY * Math.cos(negativeRotSpeed);
                break;
            case KeyEvent.VK_LEFT:
                dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
                dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);
                planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
                planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);
                break;
        }
    }

    @Override
    public synchronized void keyPressed(KeyEvent event) {
        //System.out.println("Key pressed " + event.getKeyCode());
        pressedKeys.add(event.getKeyCode());
    }

    @Override
    public synchronized void keyReleased(KeyEvent event) {
        //System.out.println("Key released " + event.getKeyCode());
        pressedKeys.remove(event.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent event) {

    }

    public void updateSize(int w, int h){
        this.width = w;
        this.height = h;
    }

    public void start(){
        if(isRunning){
            return;
        }

        isRunning = true;
        new Thread(this).start();
    }
    public void stop(){
        if(!isRunning){
            return;
        }

        isRunning = false;
    }

    public int[][] getWorldMap(){
        int[][] map = new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,2,2,2,2,2,0,0,0,0,3,0,3,0,3,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,2,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,2,0,0,0,2,0,0,0,0,3,0,0,0,3,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,2,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,2,2,0,2,2,0,0,0,0,3,0,3,0,3,0,0,0,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,4,4,4,4,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,0,4,0,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,0,0,0,0,5,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,0,4,0,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,0,4,4,4,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,4,4,4,4,4,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0}
        };
        return map;
    }
}