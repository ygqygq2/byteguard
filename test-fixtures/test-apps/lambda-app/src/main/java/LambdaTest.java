import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaTest {
    public static void main(String[] args) {
        System.out.println("ByteGuard Lambda Test Application");
        System.out.println("==================================");
        
        // Test 1: Lambda expression
        System.out.println("\n1. Testing Lambda expressions:");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        numbers.forEach(n -> System.out.println("  Number: " + n));
        
        // Test 2: Method reference
        System.out.println("\n2. Testing Method references:");
        List<String> strings = numbers.stream()
            .map(Object::toString)
            .collect(Collectors.toList());
        strings.forEach(System.out::println);
        
        // Test 3: Stream API with filter and map
        System.out.println("\n3. Testing Stream API:");
        int sum = numbers.stream()
            .filter(n -> n % 2 == 0)
            .mapToInt(Integer::intValue)
            .sum();
        System.out.println("  Sum of even numbers: " + sum);
        
        // Test 4: Custom functional interface
        System.out.println("\n4. Testing Custom functional interface:");
        MathOperation addition = (a, b) -> a + b;
        MathOperation multiplication = (a, b) -> a * b;
        
        System.out.println("  10 + 5 = " + addition.operate(10, 5));
        System.out.println("  10 * 5 = " + multiplication.operate(10, 5));
        
        // Test 5: Closure
        System.out.println("\n5. Testing Closure:");
        int factor = 10;
        MathOperation multiplyByFactor = (a, b) -> (a + b) * factor;
        System.out.println("  (3 + 7) * 10 = " + multiplyByFactor.operate(3, 7));
        
        System.out.println("\n==================================");
        System.out.println("Lambda test completed successfully!");
    }
    
    @FunctionalInterface
    interface MathOperation {
        int operate(int a, int b);
    }
}
