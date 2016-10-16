package handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import filesystem.SimplePair;
import javafx.util.Pair;

public class HtmlUploadHelper {
	public static byte[] getBoundry(byte[] input) {
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int b = 0;
		while ((b = in.read()) > 0) {
			if (b == '\n' || b == '\r')
				break;
			out.write(b);
		}

		return out.toByteArray();
	}

	public static ArrayList<File> getFormEntrys(byte[] input, byte[] boundry) {

		ArrayList<File> entrys = new ArrayList<File>();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			entrys.add(File.createTempFile("tempDownload", "downloadFile"));
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		entrys.get(entrys.size() - 1).deleteOnExit();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(entrys.get(entrys.size() - 1));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		byte bufferArray[] = new byte[boundry.length];
		int match = 0;
		for (int i = 0; i < input.length; i++) {
			byte check = input[i];
			if (check == boundry[match]) {
				bufferArray[match] = input[i];
				match++;
				if (match == boundry.length) {
					try {
						fos.write(os.toByteArray());
						os = new ByteArrayOutputStream();
						fos.flush();
						fos.close();
						entrys.add(File.createTempFile("tempDownload", "downloadFile"));
						entrys.get(entrys.size() - 1).deleteOnExit();
						fos = new FileOutputStream(entrys.get(entrys.size() - 1));
					} catch (IOException e) {
						e.printStackTrace();
					}
					match = 0;
				}
			} else {
				try {
					for (int dequeue = 0; dequeue < match; dequeue++)
						os.write(bufferArray[dequeue]);
					os.write(check);
					if (os.size() > 1024) {
						fos.write(os.toByteArray());
						os = new ByteArrayOutputStream();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				match = 0;
			}
		}
		try {
			fos.close();
		} catch (IOException e) {
		}

		return entrys;
	}

	public static ArrayList<File> getFileEntrys(ArrayList<File> input) {
		ArrayList<File> output = new ArrayList<File>();
		try {
			for (File f : input) {
				FileInputStream fin = new FileInputStream(f);
				String comp = "";
				// TODO: Maximale Headergroeße einrichten
				for (int i = 0; i < 1024 & i < fin.available(); i++) {
					comp += (char) fin.read();
				}
				fin.close();
				if (comp.contains("filename")) {
					output.add(f);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return output;
	}

	public static SimplePair<String, File> getFileEntryBytes(File input) {
		try {
			FileInputStream in = new FileInputStream(input);
			byte b[] = new byte[1024];
			in.read(b);
			String comp = new String(b);
			in.close();
			in = new FileInputStream(input);
			String fname = comp.substring(comp.indexOf("filename=\"") + "filename=\"".length());
			fname = fname.substring(0, fname.indexOf("\"")).replace("[", "").replace("]", "");
			// TODO: Test different files
			int start[] = new int[2];
			for (int i = 0; i < 4; i++) {
				start[0] = -1;
				start[1] = -1;
				while (!(start[0] == '\r' && start[1] == '\n')) {
					start[0] = start[1];
					start[1] = in.read();
				}
			}
			File f = null;
			f = File.createTempFile("someprefix", "somesuffix");
			f.deleteOnExit();
			FileOutputStream out = new FileOutputStream(f);
			byte buffer[] = new byte[1024];
			while (in.available() > 0) {
				if (in.available() < 1024)
					buffer = new byte[in.available()];
				in.read(buffer);
				out.write(buffer);
			}
			out.close();
			in.close();

			SimplePair<String, File> p = new SimplePair<String, File>(fname, f);
			// System.err.println(new String(out.toByteArray()).substring(0,
			// 10000));
			return p;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<byte[]> getMkdirEntrys(ArrayList<File> input) {
		ArrayList<byte[]> output = new ArrayList<byte[]>();
		try {
			for (File f : input) {
				FileInputStream fin = new FileInputStream(f);
				String comp = "";
				int finavMax = fin.available();
				// TODO: Maximale Headergroeße einrichten
				for (int i = 0; i < 4096 & i < finavMax; i++) {
					comp += (char) fin.read();
				}
				fin.close();
				if (comp.contains("name=\"mkdir\"")) {
					output.add(Files.readAllBytes(f.toPath()));
				}
			}
		} catch (IOException e) {

		}

		return output;
	}

	public static String getDirName(byte input[]) {
		ByteArrayInputStream in = new ByteArrayInputStream(input);
		int start[] = new int[2];
		for (int i = 0; i < 3; i++) {
			start[0] = -1;
			start[1] = -1;
			while (!(start[0] == '\r' && start[1] == '\n')) {
				start[0] = start[1];
				start[1] = in.read();
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (in.available() > 0) {
			int r = in.read();
			if (r != '\r' && r != '\n') {
				out.write(r);
			}
		}

		return new String(out.toByteArray());
	}
}
