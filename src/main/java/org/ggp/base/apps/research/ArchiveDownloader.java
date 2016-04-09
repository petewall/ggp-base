package org.ggp.base.apps.research;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class ArchiveDownloader {

	public static final String ARCHIVE_DIR = "archives";
	public static final String ARCHIVE_FILE_NAME = "match-archive-TTT-small.txt";
//	public static final String ARCHIVE_FILE_NAME = "match-archive-ALL.txt";
	public static final File ARCHIVE_FILE = new File(ARCHIVE_DIR, ARCHIVE_FILE_NAME);

	private static final String[] ARCHIVE_URLS = {
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2009.gz",
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2010.gz",
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2011.gz",
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2012.gz",
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2013.gz",
			"http://commondatastorage.googleapis.com/match-archive%2FmatchesFrom2014.gz"
	};

	private static final int BUF_SIZE = 1024 * 1024; // 1 MB
	private static final int LOG_FREQUENCY = 10 * 1024 * 1024; // 10 MB

	public static void main(String[] args) throws IOException {
		downloadArchives();
	}

	public static File getArchiveFile() {
		if(ARCHIVE_FILE.exists() == false){
			System.out.println(ArchiveDownloader.ARCHIVE_FILE + " not found so downloading all archives");
			try {
				downloadArchives();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return ARCHIVE_FILE;
	}

	public static void downloadArchives() throws IOException {
		File folder = new File(ARCHIVE_DIR);
		folder.mkdir();

		File[] downloadedFiles = new File[ARCHIVE_URLS.length];

		for (int i = 0; i < ARCHIVE_URLS.length; i++) {
			downloadedFiles[i] = downloadFile(folder, ARCHIVE_URLS[i]);
		}

		FileOutputStream destinationStream = new FileOutputStream(ARCHIVE_FILE);
		try {
			for (int i = 0; i < downloadedFiles.length; i++) {
				System.out.println("Uncompressing " + downloadedFiles[i] + " and appending to " + ARCHIVE_FILE);
				uncompressGzip(downloadedFiles[i], destinationStream);
			}
		} finally {
			if (destinationStream != null) {
				destinationStream.close();
			}
		}
		System.out.println();
		System.out.println("Downloading and uncompressing archive files complete");
		System.out.println();
	}

	private static File downloadFile(File folder, String archiveUrlStr) throws MalformedURLException, IOException, FileNotFoundException {
		System.out.println("Downloading file from URL: " + archiveUrlStr);
		URL archiveUrl = new URL(archiveUrlStr);
		File destinationFile = new File(folder, archiveUrl.getFile());
		if (destinationFile.exists()) {
			destinationFile.delete();
		}

		URLConnection conn = archiveUrl.openConnection();
		long size = conn.getContentLengthLong();
		if (size < 0) {
			System.out.println("Could not determine file size.");
		}

		BufferedInputStream inStream = new BufferedInputStream(conn.getInputStream());
		FileOutputStream outStream = new FileOutputStream(destinationFile);
		try {
			final byte buf[] = new byte[BUF_SIZE];
			int count;
			long total = 0;
			int logCount = 1;
			while ((count = inStream.read(buf, 0, BUF_SIZE)) != -1) {
				outStream.write(buf, 0, count);
				total += count;
				if (total > logCount * LOG_FREQUENCY) {
					logCount++;
					if (size < 0) {
						System.out.println(total + " bytes downloaded");
					} else {
						System.out.println(total + " of " + size + " [" + (int) (total / ((double) size) * 100) + "%] bytes downloaded");
					}
				}
			}
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (outStream != null) {
				outStream.close();
			}
		}
		System.out.println("Successfully downloaded to " + destinationFile);
		System.out.println();
		return destinationFile;
	}

	public static void uncompressGzip(File file, OutputStream outStream) throws IOException {
		GZIPInputStream gzStream = new GZIPInputStream(new FileInputStream(file));
		try {
			byte[] buf = new byte[BUF_SIZE];
			int count;
			while ((count = gzStream.read(buf, 0, BUF_SIZE)) != -1) {
				outStream.write(buf, 0, count);
			}
		} finally {
			if (gzStream != null) {
				gzStream.close();
			}
		}
	}
}
