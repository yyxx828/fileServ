package com.xajiusuo.utils;

import org.springframework.web.multipart.MultipartFile;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MD5FileUtil {

	protected static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	protected static MessageDigest messagedigest = null;
	static {
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		}catch(NoSuchAlgorithmException e) {
			System.err.println(MD5FileUtil.class.getName() + "初始化失败，MessageDigest不支持MD5Util。");
		}
	}

	private static void unMap(MappedByteBuffer buffer){
		Cleaner c = ((DirectBuffer)buffer).cleaner();
		if(c != null){
			c.clean();
		}
	}

	public static String getFileMD5String(File file) {
		FileInputStream in = null;
		FileChannel ch = null;
		try {
			in = new FileInputStream(file);
			ch = in.getChannel();
			MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			messagedigest.update(byteBuffer);
			unMap(byteBuffer);
			return bufferToHex(messagedigest.digest());
		}catch(Exception e) {
			return null;
		}finally{
			close(ch,in);
		}
	}

	public static String getFileMD5String(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bufferOut = new byte[8192];
		int bytes = 0;
		while((bytes = in.read(bufferOut)) != -1) {
			out.write(bufferOut, 0, bytes);
		}
		String md5 = getMD5String(out.toByteArray());
		close(in,out);
		return md5;
	}
	
	public static String getMD5String(String s) {
		return getMD5String(s.getBytes());
	}

	public static String getMD5BufferedImage(BufferedImage image) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpg", bos);
		}catch(IOException e) {
			return null;
		}
		return MD5FileUtil.getMD5String(bos.toByteArray());
	}
	
	public static String getMD5String(byte[] bytes) {
		messagedigest.update(bytes);
		return bufferToHex(messagedigest.digest());
	}

	private static String bufferToHex(byte bytes[]) {
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n) {
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for(int l = m; l < k; l++) {
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}

	public static File newFile(File file){
		if(file != null){
			if(file.exists()){
				return file;
			}
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();
			} catch (IOException e) {
			}
		}
		return file;
	}

	public static File newFile(String path,String fileName){
		return newFile(new File(path,fileName));
	}


	public static File newFile(String fileName){
		return newFile(new File(fileName));
	}

	public static File newPath(File file){
		if(file.isFile()){
			file.getParentFile().mkdirs();
		}else if(file.isDirectory()){
			file.mkdirs();
		}else if(!file.isFile() && !file.isDirectory()){
			file.getParentFile().mkdirs();
		}
		return file;
	}

	public static File newPath(String fileName){
		return newPath(new File(fileName));
	}

	public static void close(AutoCloseable...cs){
		for(AutoCloseable c:cs){
			try{
				if(c != null){
					if(c instanceof Flushable){
						((Flushable) c).flush();
					}
					c.close();
				}
			}catch (Exception e){}
		}
	}

	private static ExecutorService pool = Executors.newFixedThreadPool(3);



	public static void copys(File from,File to){
		if(from.isFile()){
			if(!to.exists()){
				copyf(from,to);
			}
		}else if(from.isDirectory()){
			to.mkdirs();
			to = new File(to,from.getName());
			for(File f:from.listFiles()){
				copys(f,new File(to.getParent(),f.getName()));
			}
		}
	}

	public static void delete(String fileName){
		delete(new File(fileName));
	}

	public static void delete(File file){
		if(!file.exists()) return;
		if(file.isFile()){
			file.delete();
		}else{
			for(File f:file.listFiles()){
				delete(f);
			}
			file.delete();
		}
	}

	public static void deleteChildren(String fileName){
		deleteChildren(new File(fileName));
	}


	public static void deleteChildren(File file){
		if(file.isDirectory()){
			for(File f:file.listFiles()){
				delete(f);
			}
		}
	}

	/***
	 * 文件复制
	 * @param from 源文件
	 * @param to 目标文件
     */
	public static void copyf(File from, File to){
		OutputStream out = null;
		try{
			copyf(new FileInputStream(from),out = new FileOutputStream(newFile(to)));
		}catch (Exception e){
		}finally {
			MD5FileUtil.close(out);
		}
	}

	/***
	 * 通过流进行复制
	 * @param in 输入流 用完后会自动关闭
	 * @param out 输出流,用完不进行关闭
     */
	public static void copyf(InputStream in, OutputStream out){
		try{
			byte[] buff = new byte[8192];
			int len = 0;
			while((len = in.read(buff)) != -1){
				out.write(buff,0,len);
			}
		}catch (Exception e){
		}finally {
			close(in);
		}
	}

	/***
	 * 文件大小
	 * @param file
	 * @return
     */
	public static long fileSize(File file) {
		long size = 0;
		if(file.isFile()){
			return file.length();
		}else if(file.isDirectory() && file.listFiles() != null){
			for(File f:file.listFiles()){
				size += fileSize(f);
			}
		}
		return size;
	}

	/***
	 * 文件大小
	 * @param file
	 * @return
	 */
	public static int fileNum(File file,String exclusion) {
		int size = 0;
		if(file.isFile()){
			return 1;
		}else if((StringUtils.isBlank(exclusion) && file.isDirectory()) || (StringUtils.isNotBlank(exclusion) && !file.getName().equals(exclusion))){
			int tsize = 0;
			for(File f:file.listFiles()){
				if(f.isFile()) tsize++;
				else tsize += fileNum(f,exclusion);
			}
			size += tsize;
		}
		return size;
	}




	private static char[] sizeUnit = new char[]{'B','K','M','G','T','P'};
	private static NumberFormat numFormat = new DecimalFormat("#,###.##");
	private static Map<String, Long> map = new HashMap<String, Long>(10);
	static{
		map.put("B", 1L);
		map.put("K", 1024L);
		map.put("M", 1024L * 1024);
		map.put("G", 1024L * 1024 * 1024);
		map.put("T", 1024L * 1024 * 1024 * 1024);
		map.put("P", 1024L * 1024 * 1024 * 1024 * 1024);
	}

	/***
	 * 对文件大小进行格式化显示
	 * @return
	 */
	public static String l2s(long fileSize){
		double len = fileSize;
		int r = 0;
		while(len > 1024 && r < 4){
			r ++;
			len /= 1024;
		}
		return numFormat.format(len) + sizeUnit[r];
	}

	public static String getContentType(File file) {
		try {
			return Files.probeContentType(Paths.get(file.toURI()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 文件拿取后要自行删除,否则会常驻硬盘,开发过程请严格检查是否文件能正常删除
	 * @param file
	 * @return
     */
	public static File tempSave(MultipartFile file){
		String name = file.getOriginalFilename();

		String ext = ".tmp";
		if(name.contains(".")){
			ext = name.substring(name.lastIndexOf("."));
		}

		//清理3天未处理的文件
		File dir = path();
		if(dir.isDirectory() && dir.exists()){
			for(File f:dir.listFiles()){
				if(System.currentTimeMillis() - f.lastModified() > 3 * 24 * 60 * 60 * 1000){
					f.delete();
				}
			}
		}

		File temp = new File(path(),System.currentTimeMillis()  + ext);

		try {
			file.transferTo(temp);
		}catch (Exception e){
		}

		return temp;
	}

	/***
	 * 文件拿取后要自行删除,否则会常驻硬盘,开发过程请严格检查是否文件能正常删除
	 * @param file
	 * @return
	 */
	public static File tempSave(InputStream in,String url){
		String ext = ".urlDown";
		if(url.contains("/")){
			url = url.substring(url.lastIndexOf("/") + 1);
			if(url.contains(".")){
				ext = url.substring(url.lastIndexOf("."));
			}
		}

		File dir = path();
		if(dir.isDirectory() && dir.exists()){
			for(File f:dir.listFiles()){
				if(System.currentTimeMillis() - f.lastModified() > 3 * 24 * 60 * 60 * 1000){
					f.delete();
				}
			}
		}

		File temp = new File(path(),System.currentTimeMillis()  + ext);

		try {
			temp.mkdirs();
			temp.createNewFile();
			FileOutputStream out = new FileOutputStream(temp);
			copyf(in,out);
			close(out);
		}catch (Exception e){
		}

		temp.deleteOnExit();

		return temp;
	}

	public static InputStream getIn(File file){
		try {
			return new FileInputStream(file);
		}catch (Exception e){
			return null;
		}
	}

	private static File sysTempPath = null;

	/**
	 * 获取临时文件路径,单次上传之后删除
	 * @return
     */
	public static File path(){

		if(sysTempPath != null){
			return sysTempPath;
		}

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL url = null;
		try {
			Enumeration<URL> urls = loader.getResources("");

			url = urls.nextElement();

			sysTempPath = new File(url.getFile()).getParentFile().getParentFile();
		}catch (Exception e){
			sysTempPath = new File(url.getFile());
		}

		sysTempPath = new File(sysTempPath,"tempUpload");

		if(!sysTempPath.exists()){
			sysTempPath.mkdirs();
		}

		return sysTempPath;
	}

	private static String tempPath = null;

	public String getTempPath(){
		if(StringUtils.isBlank(tempPath)){
			String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replace("file:","");
			File parent = new File(path);
			if(path.contains(".jar!") || path.contains(".war")){
				while(parent.getAbsolutePath() != null &&(parent.getAbsolutePath().contains(".jar!") || parent.getAbsolutePath().contains(".war"))){
					parent = parent.getParentFile();
				}
				tempPath = parent.getParentFile().getAbsolutePath();
			}else{
				tempPath = parent.getParentFile().getParentFile().getParent();
			}
			tempPath += "/temp";
		}
		return tempPath;
	}

	public File crateFile(String exampleName){
		String ext = "";
		if(exampleName != null && exampleName.contains(".")){
			ext = exampleName.substring(exampleName.lastIndexOf("."));
		}
		File file = new File(getTempPath(),System.currentTimeMillis() + (1000 + new Random().nextInt(10000)) + ext);
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
			return file;
		} catch (IOException e) {
		}
		return null;
	}


	/***
	 * 将in内文件增加到out文件中
	 * @param inFile
	 * @param outFile
     */
	public static void append(File inFile,File outFile){
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"utf-8"));
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile,true)));
			String line = null;
			while((line = in.readLine()) != null){
				if(StringUtils.isNotBlank(line)) out.write(line + "\r\n");
			}
		} catch (Exception e) {
		}finally {
			close(in,out);
		}
	}

	public static String pwdMd5(String pwd){
		String md5 = getMD5String(pwd);
		String md51 = md5 + md5;
		StringBuilder sb = new StringBuilder();
		for(int i =0;i<md5.length();i++){
			sb.append(MD5FileUtil.getMD5String(md51.substring(i,i+3)));
		}
		return sb.toString();
	}


}
