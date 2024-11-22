package jade;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import renderer.DebugDraw;
import renderer.Framebuffer;
import scenes.LevelEditorScene;
import scenes.LevelScene;
import scenes.Scene;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
public class Window {

    private int width, height;
    private String title;
    private long glfwWindow;
    private ImGuiLayer imGuiLayer;

    public float r, g, b, a;

    private static Window window = null;

    private static Scene currentScene;
    private Framebuffer framebuffer;

    private Window() {
        this.width = 1920;
        this.height = 1080;
        this.title = "Mario";
        r = 0;
        g = 0;
        b = 0;
        a = 1;
    }

    public static void changeScene(int newScene) {
        switch (newScene) {
            case 0:
                currentScene = new LevelEditorScene();
//                currentScene.init();
//                currentScene.start();
                break;
            case 1:
                currentScene = new LevelScene();
//                currentScene.init();
//                currentScene.start();
                break;
            default:
                assert false : "Unknown Scene: '" + newScene + "'";
                break;
        }
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

        // Free the memory (OS will do this, but this is nicer)
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);

        // Terminate GLFW and free the error callback (OS will do this, but this is
        // nicer)
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
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

        // Create the window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create the GLFW window.");
        }

        // Setting our callbacks for the mouse and keyboard
        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
//         glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
//         Window.setWidth(newWidth);
//         Window.setHeight(newHeight);
//         });

        // Make OpenGL the context current
        glfwMakeContextCurrent(glfwWindow);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(glfwWindow);

        // This row is very important because a very long text on their site!
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        this.imGuiLayer = new ImGuiLayer(glfwWindow);
        this.imGuiLayer.initImGui();
        // unCMT dong nay de tat buffer
//        this.framebuffer = new Framebuffer(3840, 2160);
        //========================
        Window.changeScene(0);
    }

    public void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;

//        currentScene.load();
        while (!glfwWindowShouldClose(glfwWindow)) {
            // Poll events
            glfwPollEvents();
            DebugDraw.beginFrame();

            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);
            // unCMT dong nay de tat buffer
//            this.framebuffer.bind();
            //---------------------
            if (dt >= 0) {
                DebugDraw.draw();
                currentScene.update(dt);
            }
            // unCMT dong nay de tat buffer
//            this.framebuffer.unbind();
            //---------------------
//            this.imGuiLayer.update(dt);
            this.imGuiLayer.update(dt, currentScene);
            glfwSwapBuffers(glfwWindow);

            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
        currentScene.saveExit();
    }

    public static int getWidth() {
        return Window.get().width;
    }

    public static int getHeight() {
        return Window.get().height;
    }

    private static void setWidth(int newWidth) {
        get().width = newWidth;
    }

    private static void setHeight(int newHeight) {
        get().height = newHeight;
    }
}