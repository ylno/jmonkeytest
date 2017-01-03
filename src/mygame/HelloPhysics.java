package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.shadow.DirectionalLightShadowRenderer;

/**
 * Example 12 - how to give objects physical properties so they bounce and fall.
 *
 * @author base code by double1984, updated by zathras
 */
public class HelloPhysics extends SimpleApplication implements ActionListener {

    public static void main(String args[]) {
        HelloPhysics app = new HelloPhysics();
        app.start();
    }

    /**
     * Prepare the Physics Application State (jBullet)
     */
    private BulletAppState bulletAppState;

    /**
     * Prepare Materials
     */
    Material wall_mat;
    Material stone_mat;
    Material floor_mat;

    /**
     * Prepare geometries and physical nodes for bricks and cannon balls.
     */
    private RigidBodyControl brick_phy;
    private static final Box box;
    private RigidBodyControl ball_phy;
    private static final Sphere sphere;
    private RigidBodyControl floor_phy;
    private static final Box floor;

    /**
     * dimensions used for bricks and wall
     */
    private static final float brickLength = 0.48f;
    private static final float brickWidth = 0.24f;
    private static final float brickHeight = 0.12f;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;

    private CharacterControl player;

    static {
        /**
         * Initialize the cannon ball geometry
         */
        sphere = new Sphere(32, 32, 0.4f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
        /**
         * Initialize the brick geometry
         */
        box = new Box(brickLength, brickHeight, brickWidth);
        box.scaleTextureCoordinates(new Vector2f(1f, .5f));
        /**
         * Initialize the floor geometry
         */
        floor = new Box(10f, 0.1f, 10f);
        floor.scaleTextureCoordinates(new Vector2f(3, 6));
    }

    @Override
    public void simpleInitApp() {
        /**
         * Set up Physics Game
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        /**
         * Configure cam to look at scene
         */
        //cam.setLocation(new Vector3f(0, 4f, 6f));
        //cam.lookAt(new Vector3f(2, 2, 0), Vector3f.UNIT_Y);
        /**
         * Add InputManager action: Left click triggers shooting.
         */
        inputManager.addMapping("shoot",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");

        /**
         * Initialize the scene, materials, and physics space
         */
        initMaterials();
        initWall();
        initFloor();
        initCrossHairs();
        makeStarField();

        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.0f, 1f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(5);

        player.setFallSpeed(50);
        player.setGravity(30);
        player.setPhysicsLocation(new Vector3f(0, 10f, 4f));
        cam.setLocation(player.getPhysicsLocation());

        bulletAppState.getPhysicsSpace().add(player);
        flyCam.setMoveSpeed(10);

        /**
         * A white, directional light source
         */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        /* this shadow needs a directional light */
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 2);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

    }

    /**
     * Every time the shoot action is triggered, a new cannon ball is produced.
     * The ball is set up to fly from the camera position in the camera
     * direction.
     */
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            System.out.println("actionlistener " + name);
            if (name.equals("shoot") && !keyPressed) {
                makeCannonBall();
            }

            if (name.equals("Left")) {
                left = keyPressed;
            } else if (name.equals("Right")) {
                right = keyPressed;
            } else if (name.equals("Up")) {
                up = keyPressed;
            } else if (name.equals("Down")) {
                down = keyPressed;
            } else if (name.equals("Jump")) {
                if (keyPressed) {
                    player.jump();
                }
            }
        }
    };

    /**
     * Initialize the materials used in this scene.
     */
    public void initMaterials() {
        wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        wall_mat.setTexture("ColorMap", tex);

        stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        stone_mat.setTexture("ColorMap", tex2);

        floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(WrapMode.Repeat);
        floor_mat.setTexture("ColorMap", tex3);
    }

    /**
     * Make a solid floor and add it to the scene.
     */
    public void initFloor() {
        Geometry floor_geo = new Geometry("Floor", floor);
        floor_geo.setMaterial(floor_mat);
        floor_geo.setLocalTranslation(0, -0.1f, 0);
        this.rootNode.attachChild(floor_geo);
        /* Make the floor physical with mass 0.0f! */
        floor_phy = new RigidBodyControl(0.0f);
        floor_geo.addControl(floor_phy);
        bulletAppState.getPhysicsSpace().add(floor_phy);
    }

    /**
     * This loop builds a wall out of individual bricks.
     */
    public void initWall() {
        float startpt = brickLength / 4;
        float height = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                Vector3f vt
                        = new Vector3f(i * brickLength * 2 + startpt, brickHeight + height, 0);
                makeBrick(vt);
            }
            startpt = -startpt;
            height += 2 * brickHeight;
        }
    }

    /**
     * This method creates one individual physical brick.
     */
    public void makeBrick(Vector3f loc) {
        /**
         * Create a brick geometry and attach to scene graph.
         */
        Geometry brick_geo = new Geometry("brick", box);
        brick_geo.setMaterial(wall_mat);
        rootNode.attachChild(brick_geo);
        /**
         * Position the brick geometry
         */
        brick_geo.setLocalTranslation(loc);
        /**
         * Make brick physical with a mass > 0.0f.
         */
        brick_phy = new RigidBodyControl(4f);
        /**
         * Add physical brick to physics space.
         */
        brick_geo.addControl(brick_phy);
        bulletAppState.getPhysicsSpace().add(brick_phy);
    }

    /**
     * This method creates one individual physical cannon ball. By defaul, the
     * ball is accelerated and flies from the camera position in the camera
     * direction.
     */
    public void makeCannonBall() {
        /**
         * Create a cannon ball geometry and attach to scene graph.
         */
        Geometry ball_geo = new Geometry("cannon ball", sphere);
        ball_geo.setMaterial(stone_mat);
        ball_geo.scale(.3f);
        rootNode.attachChild(ball_geo);
        /**
         * Position the cannon ball
         */
        final Vector3f direction = cam.getDirection();
        final Vector3f location = cam.getLocation();
        ball_geo.setLocalTranslation(location.add(direction));
        /**
         * Make the ball physcial with a mass > 0.0f
         */
        ball_phy = new RigidBodyControl(.1f);
        /**
         * Add physical ball to physics space.
         */
        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);
        /**
         * Accelerate the physcial ball to shoot it.
         */
        ball_phy.setLinearVelocity(direction.mult(25));
    }

    /**
     * A plus sign used as crosshairs to help the player with aiming.
     */
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");        // fake crosshairs :)
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.1f);
        camLeft.set(cam.getLeft()).multLocal(0.1f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        System.out.println("onAction " + binding);
        if (binding.equals("Left")) {
            left = isPressed;
        } else if (binding.equals("Right")) {
            right = isPressed;
        } else if (binding.equals("Up")) {
            up = isPressed;
        } else if (binding.equals("Down")) {
            down = isPressed;
        } else if (binding.equals("Jump")) {
            if (isPressed) {
                player.jump();
            }
        }
    }

    private void makeStarField() {
        final ParticleEmitter emit = new ParticleEmitter("Emitter", Type.Point, 10000);
        emit.setShape(new EmitterBoxShape(new Vector3f(-180f, -180f, -180f),
                new Vector3f(180f, 180f, 180f)));
        emit.setGravity(0, 0, 0);
        emit.setLowLife(60);
        emit.setHighLife(60);
        emit.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0, 0));
        emit.setImagesX(15);
        emit.setStartSize(0.05f);
        emit.setEndSize(0.05f);
        emit.setStartColor(ColorRGBA.White);
        emit.setEndColor(ColorRGBA.White);
        emit.setSelectRandomImage(true);
        emit.emitAllParticles();

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setBoolean("PointSprite", true);
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
        emit.setMaterial(mat);

        rootNode.attachChild(emit);
    }
}
