package com.juanra.ReportsJRMiniServer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import com.juanra.ReportsJRMiniServer.model.Conexion;
import com.juanra.ReportsJRMiniServer.model.Constants;

/**
 * Aplicacion de generacion de informes a partir de ultimas
 * versiones de JRXML con las ultimas versiones de las 
 * librerias de JasperReports 
 *
 */
public class MiniJRServer {

	/**
	 * Metodo de entrada con los argumentos necesarios para la
	 * realizacion del informe
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String rutaPDF = "";
		String rutaJRXML = "";
		
		try {
			Conexion conexion = new Conexion();
			
			// Crear el Mapa de Parametros desde el argumento
			HashMap<String, Object> params = new HashMap<String, Object>();
			//
			HashMap<String, Object> imagenesMap = null;
			String imagenesSt = null;
			
			
			for (int i = 0; i < args.length; i++) {
				// System.out.println(args[i]);

				//
				String argumento = args[i];
				Scanner sc = new Scanner(argumento);
				sc.useDelimiter(Constants.CHAR_IGUAL);
				String argumentoClave = sc.next();
				String argumentoValor = sc.next();
				sc.close();

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_BBDD_DRIVER)) {
					conexion.setDriver(argumentoValor);
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_BBDD_URL)) {
					conexion.setUrl(argumentoValor);
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_BBDD_USER)) {
					conexion.setUser(argumentoValor);
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_BBDD_PASS)) {
					conexion.setPass(argumentoValor);
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_RUTA_JRXML)) {
					rutaJRXML = argumentoValor;
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_RUTA_PDF)) {
					rutaPDF = argumentoValor;
				}

				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_LISTA_PARAMS)) {
					params = procesarParametros(argumentoValor);
				}
				
				if (argumentoClave.equalsIgnoreCase(Constants.PARAM_LISTA_IMAGENES)) {
					if (!rutaPDF.equalsIgnoreCase("")) {
						imagenesMap = procesarImagenes(argumentoValor, rutaPDF);
						params.putAll(imagenesMap);
					} else {
						imagenesSt = argumentoValor;
					}
				}
			}
			
			if (imagenesMap == null) {
				imagenesMap = procesarImagenes(imagenesSt, rutaPDF);
				params.putAll(imagenesMap);
			}
			
			// Crear conexion
			Class.forName(conexion.getDriver()).newInstance();
			Connection conn = DriverManager.getConnection(conexion.getUrl(),
					conexion.getUser(), conexion.getPass());

			/*
			 * Statement stm = conn.createStatement(); ResultSet queryResult =
			 * stm.executeQuery(
			 * "select TABLA, PARAMETRO, DESCRIPCI from MAETABLA where tabla = 1 order by tabla"
			 * );
			 * 
			 * while (queryResult.next()) {
			 * System.out.println(queryResult.getString("TABLA") + " - " +
			 * queryResult.getString("PARAMETRO") + " - " +
			 * queryResult.getString("DESCRIPCI")); }
			 */

			// Crear el informe pdf en una ruta temporal

			JasperReport jr = JasperCompileManager.compileReport(rutaJRXML);
			JasperPrint jp = JasperFillManager.fillReport(jr, params, conn);

			byte[] bytesPDF = JasperExportManager.exportReportToPdf(jp);

			BufferedOutputStream bw = new BufferedOutputStream(
					new FileOutputStream(new File(rutaPDF)));
			bw.write(bytesPDF);
			bw.close();

			// Lanzarlo para que se vea en pantalla
			/*
			 * String[] parametrosLlamada = new String[2]; parametrosLlamada[0]
			 * = "gnome-open"; parametrosLlamada[1] = rutaPDF;
			 * Runtime.getRuntime().exec(parametrosLlamada);
			 */

		} catch (Exception ex) {
			File f = new File(rutaPDF + Constants.ERROR_EXT);
			f.createNewFile();
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param argumentoValor
	 * @return
	 * @throws Exception
	 */
	private static HashMap<String, Object> procesarParametros(
			String argumentoValor) throws Exception {
		HashMap<String, Object> params = new HashMap<String, Object>();

		Scanner sc = new Scanner(argumentoValor);
		sc.useDelimiter(Constants.CHAR_ARROBA);

		while (sc.hasNext()) {

			// Segundo Scanner
			Scanner sc2 = new Scanner(sc.next());
			sc2.useDelimiter(Constants.CHAR_ALMHOADILLA);
			@SuppressWarnings("rawtypes")
			Class clase = Class.forName(sc2.next());
			String key = sc2.next();
			String value = sc2.next();
			Object o = null;
			if (clase.getName().equalsIgnoreCase(String.class.getName())) {
				o = value;
			} else if (clase.getName().equalsIgnoreCase(Integer.class.getName())) {
				o = new Integer(value);
			} else if (clase.getName().equalsIgnoreCase(Short.class.getName())) {
				o = new Short(value);
			} else if (clase.getName().equalsIgnoreCase(Long.class.getName())) {
				o = new Long(value);
			} else if (clase.getName().equalsIgnoreCase(Date.class.getName())) {
				SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DDMMYYYY);
				o = sdf.parse(value);
			} else if (clase.getName().equalsIgnoreCase(BigDecimal.class.getName())) {
				BigDecimal bd = new BigDecimal(value);
				o = bd;
			}
			sc2.close();
			params.put(key, clase.cast(o));
		}
		sc.close();
		return params;
	}
	
	/**
	 * 
	 * @param argumentoValor
	 * @param rutaPDF
	 * @return
	 */
	private static HashMap<String, Object> procesarImagenes (
			String argumentoValor, String rutaPDF) {
		HashMap<String, Object> mapImagenes = new HashMap<String, Object>();
		
		File pdf = new File(rutaPDF);
		String pathDirectory = pdf.getParent();
		
		Scanner sc = new Scanner(argumentoValor);
		sc.useDelimiter(Constants.CHAR_ARROBA);

		while (sc.hasNext()) {
			
			Scanner sc2 = new Scanner(sc.next());
			sc2.useDelimiter(Constants.CHAR_ALMHOADILLA);
			
			while(sc2.hasNext()) {
				try {
					String key = sc2.next();
					String extension = sc2.next();
					mapImagenes.put(key, new FileInputStream(new File(pathDirectory + File.separator + key + Constants.CHAR_PUNTO + extension)));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			sc2.close();
			
		}
		
		sc.close();
		return mapImagenes;
	}
}
