import sri.Sri;

public class Example {
    public static void main(String[] args) {
        System.out.println(Sri.eval("10 + 20"));
        System.out.println(Sri.eval("'hello'.upcase"));
        System.out.println(Sri.eval("[1, 2, 3, 4, 5].map { |x| x * 2 }"));
    }
}
