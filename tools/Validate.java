import java.io.File;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Validates watchface.xml against the official Watch Face Format v5 schema
 * (vendored from github.com/google/watchface into tools/wff, Apache 2.0).
 * This is the same XSD 1.1 check the official wff-validator.jar performs.
 *
 * Run from the project root:
 *   java -cp "tools/wff/lib/*" tools/Validate.java [path/to/watchface.xml]
 */
public class Validate {
    public static void main(String[] args) throws Exception {
        String xml = args.length > 0 ? args[0] : "app/src/main/res/raw/watchface.xml";
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Validator validator = factory.newSchema(new File("tools/wff/xsd-5/watchface.xsd")).newValidator();
        int[] errors = {0};
        validator.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException e) { report("warning", e); }
            public void error(SAXParseException e) { errors[0]++; report("error", e); }
            public void fatalError(SAXParseException e) { errors[0]++; report("fatal", e); }
        });
        validator.validate(new StreamSource(new File(xml)));
        if (errors[0] == 0) {
            System.out.println("VALID: " + xml + " conforms to Watch Face Format v5");
        } else {
            System.out.println("INVALID: " + errors[0] + " error(s)");
            System.exit(1);
        }
    }

    static void report(String level, SAXParseException e) {
        System.out.printf("%s line %d: %s%n", level, e.getLineNumber(), e.getMessage());
    }
}
