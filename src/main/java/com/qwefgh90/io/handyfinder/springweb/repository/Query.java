package com.qwefgh90.io.handyfinder.springweb.repository;

public class Query {
	static enum TABLE_NAMES{
		DIRECTORY;
	}
	
	public final static String createTable(String tableName){
		if(TABLE_NAMES.DIRECTORY.name().equals(tableName))
			return "create table " + tableName + "(pathString varchar(255), used BOOLEAN, recursively BOOLEAN)";
		else
			throw new RuntimeException(tableName+" is not a item in our list");
	}

	public final static String dropTable(String tableName){
		if(TABLE_NAMES.DIRECTORY.name().equals(tableName))
			return "drop table " + tableName;
		else
			throw new RuntimeException(tableName+" is not a item in our list");
	}
	
	public final static String tableList()
	{
		return "SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE='T'";
	}
	
	public final static String tableListWhere()
	{
		return "SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE='T' AND TABLENAME = ?";
	}
	
	/**
	 * DIRECTORY
	 * @return
	 */
	public final static String insertDirectory(){
		return "INSERT INTO DIRECTORY VALUES(?,?,?)";
	}

	/**
	 * DIRECTORY
	 * @return
	 */
	public final static String deleteDirectoryWhere(){
		return "DELETE FROM DIRECTORY WHERE pathString=?";
	}

	/**
	 * DIRECTORY
	 * @return
	 */
	public final static String deleteDirectories(){
		return "DELETE FROM DIRECTORY";
	}

	/**
	 * DIRECTORY
	 * @return
	 */
	public final static String selectDirectory(){
		return "SELECT pathString, used, recursively FROM DIRECTORY"; 
	}
}
