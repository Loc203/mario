import jade.ImGuiLayer1;
import jade.Window1;

public class Main1 {
    public static void main(String[] args) {
        Window1 window = new Window1(new ImGuiLayer1());
        window.init();
        window.run();
        window.destroy();
    }
}