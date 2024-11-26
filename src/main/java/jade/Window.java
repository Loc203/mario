package jade;

import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import physics2d.Physics2D;
import renderer.*;
import scenes.LevelEditorSceneInitializer;
import scenes.LevelSceneInitializer;
import scenes.Scene;
import scenes.SceneInitializer;
import util.AssetPool;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;
public class Window implements Observer {

    private int width, height;
    private String title;
    private long glfwWindow;
    private ImGuiLayer imGuiLayer;

    private boolean runtimePlaying = false;
    private static Window window = null;

    private static Scene currentScene;
    private Framebuffer framebuffer;
    private PickingTexture pickingTexture;
    private long audioContext;
    private long audioDevice;
    // chinh thanh false de chinh sua tro choi
    public static final boolean RELEASE_BUILD = true;
//    public static final boolean RELEASE_BUILD = false;

    private Window() {
        this.width = 1920;
        this.height = 1080;
//        this.width = 2560;
//        this.height = 1440;
        this.title = "Mario Game";
        EventSystem.addObserver(this);
    }

    public static void changeScene(SceneInitializer sceneInitializer) {
        if (currentScene != null) {
            currentScene.destroy();

        }
        if (!RELEASE_BUILD) {
            getImguiLayer().getPropertiesWindow().setActiveGameObject(null);
        }
        currentScene = new Scene(sceneInitializer);
        currentScene.load();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        if (window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public static Scene getScene() {
        return get().currentScene;
    }

    public void run() {
        init();
        loop();
        //audio
        alcDestroyContext(audioContext);
        alcCloseDevice(audioDevice);
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        // Configure GLFW
        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
//        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create the GLFW window.");
        }

        // Setting callbacks for the mouse and keyboard
        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
//         glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
//         Window.setWidth(newWidth);
//         Window.setHeight(newHeight);
//         });


        glfwMakeContextCurrent(glfwWindow);
        glfwSwapInterval(1);

        //window visible
        glfwShowWindow(glfwWindow);

        // init audio device
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);
        int[] attributes = {0};
        audioContext = alcCreateContext(audioDevice, attributes);
        alcMakeContextCurrent(audioContext);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if (!alCapabilities.OpenAL10) {
            assert false : "Audio library not supported.";
        }

        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        // unCMT dong nay de tat buffer
        this.framebuffer = new Framebuffer(3840, 2160);
        this.pickingTexture = new PickingTexture(3840, 2160);
        glViewport(0, 0, 3840, 2160);
        //========================
        if (RELEASE_BUILD) {
            runtimePlaying = true;
            Window.changeScene(new LevelSceneInitializer());
        } else {
            this.imGuiLayer = new ImGuiLayer(glfwWindow, pickingTexture);
            this.imGuiLayer.initImGui();
            Window.changeScene(new LevelEditorSceneInitializer());
        }
    }

    public void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;
        Shader defaultShader = AssetPool.getShader("assets/shaders/default.glsl");
        Shader pickingShader = AssetPool.getShader("assets/shaders/pickingShader.glsl");
//        currentScene.load();
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwPollEvents();

            // render to picking texture
            glDisable(GL_BLEND);
            pickingTexture.enableWriting();

            glViewport(0, 0, 3840, 2160);
            glClearColor(0, 0, 0, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Renderer.bindShader(pickingShader);
            currentScene.render();

            pickingTexture.disableWriting();
            glEnable(GL_BLEND);

            DebugDraw.beginFrame();

            // unCMT dong nay de tat buffer
            this.framebuffer.bind();
            Vector4f clearColor = currentScene.camera().clearColor;
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
            glClear(GL_COLOR_BUFFER_BIT);
            //---------------------
            if (dt >= 0) {
                Renderer.bindShader(defaultShader);
                if (runtimePlaying) {
                    currentScene.update(dt);
                } else {
                    currentScene.editorUpdate(dt);
                }
                currentScene.render();
                DebugDraw.draw();
            }
            // unCMT dong nay de tat buffer
            this.framebuffer.unbind();
            //---------------------
//            this.imGuiLayer.update(dt);
            if (RELEASE_BUILD) {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer.getFboID());
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
                glBlitFramebuffer(0, 0, framebuffer.width, framebuffer.height, 0, 0, this.width, this.height,
                        GL_COLOR_BUFFER_BIT, GL_NEAREST);
            } else {
                this.imGuiLayer.update(dt, currentScene);
            }
            KeyListener.endFrame();
            MouseListener.endFrame();
            glfwSwapBuffers(glfwWindow);
            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

    public static int getWidth() {
        return Window.get().width;
//        return 3840;
    }

    public static int getHeight() {
        return Window.get().height;
//        return 2160;
    }

    private static void setWidth(int newWidth) {
        get().width = newWidth;
    }

    private static void setHeight(int newHeight) {
        get().height = newHeight;
    }

    public static Framebuffer getFramebuffer() {
        return get().framebuffer;
    }

    public static float getTargetAspectRatio() {
        return 16.0f / 9.0f;
    }
    public static ImGuiLayer getImguiLayer() {
        return get().imGuiLayer;
    }
    @Override
    public void onNotify(GameObject object, Event event) {
        switch (event.type) {
            case GameEngineStartPlay:
                this.runtimePlaying = true;
                currentScene.save();
                Window.changeScene(new LevelSceneInitializer());
                break;
            case GameEngineStopPlay:
                this.runtimePlaying = false;
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case LoadLevel:
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case SaveLevel:
                currentScene.save();
                break;
        }
    }
    public static Physics2D getPhysics() { return currentScene.getPhysics(); }
}