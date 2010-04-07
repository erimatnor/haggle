import javax.swing.JFileChooser;
import java.io.File;

public class openfile {

	public static void main(String[] args) {
		final JFileChooser fc = new JFileChooser();
		if(args.length > 0)
		    fc.setCurrentDirectory(new File(args[0]));
		int returnVal = fc.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			System.out.println(fc.getSelectedFile().getPath());
		}
	}
}
