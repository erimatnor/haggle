import java.io.File;

import javax.swing.JFileChooser;


public class openfile {
    public static void main(String[] args) {
        final JFileChooser fc = new JFileChooser();

        if (args.length > 0) {
            fc.setCurrentDirectory(new File(args[0]));
        }

        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println(fc.getSelectedFile().getPath());
        }
    }
}
