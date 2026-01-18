public class SimpleMain {
    public static void main(String[] args) {
        System.out.println("ByteGuard Simple Test Application");
        System.out.println("=================================");
        
        Calculator calc = new Calculator();
        System.out.println("2 + 3 = " + calc.add(2, 3));
        System.out.println("10 - 4 = " + calc.subtract(10, 4));
        System.out.println("5 * 6 = " + calc.multiply(5, 6));
        System.out.println("20 / 4 = " + calc.divide(20, 4));
        
        Greeter greeter = new Greeter("ByteGuard");
        greeter.greet();
        
        System.out.println("=================================");
        System.out.println("Test completed successfully!");
    }
}

class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return a / b;
    }
}

class Greeter {
    private final String name;
    
    public Greeter(String name) {
        this.name = name;
    }
    
    public void greet() {
        System.out.println("Hello from " + name + "!");
    }
}
