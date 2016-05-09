package com.qwefgh90.io.handyfinder.springweb.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import com.qwefgh90.io.handfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.repository.Query.TABLE_NAMES;

/**
 * CRUD of index properties
 * 
 * @author choechangwon
 *
 */
@Repository
@DependsOn({ "dataSource" })
public class IndexProperty {

	@Autowired
	@Qualifier("dataSource")
	DataSource dataSource;

	/**
	 * table create
	 * 
	 * @throws SQLException
	 *             throw Exception to Servlet Dispatcher
	 */
	@PostConstruct
	public void setup() throws SQLException {
		createDirectoryTable();
	}

	/**
	 * dbname of TABLE_NAMES which is enum class
	 * 
	 * @param dbname
	 * @return
	 * @throws SQLException
	 *             throw Exception to Servlet Dispatcher
	 */
	public boolean isExistDB(String dbname) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement(Query.tableListWhere());
			pstmt.setString(1, dbname);
			rs = pstmt.executeQuery();
			if (rs.next() == false)
				return false;
			String tableName = rs.getString(1);
			if (tableName == null || !tableName.equals(dbname)) {
				return false;
			}
			return true;
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (rs != null)
				rs.close();
			if (conn != null)
				conn.close();
		}
	}

	public void save(List<Directory> list) throws SQLException {
		for (Directory dir : list) {
			saveOne(dir);
		}
	}

	public void saveOne(Directory dir) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement(Query.insertDirectory());
			pstmt.setString(1, dir.getPathString());
			pstmt.setBoolean(2, dir.isUsed());
			pstmt.setBoolean(3, dir.isRecusively());
			pstmt.executeUpdate();
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}
	}

	public void deleteOne(Directory dir) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement(Query.deleteDirectoryWhere());
			pstmt.setString(1, dir.getPathString());
			pstmt.executeUpdate();
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}
	}

	public void deleteDirectories() throws SQLException {
		try (Connection conn = dataSource.getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(Query.deleteDirectories());
			}
		}
	}

	public List<Directory> selectDirectory() throws SQLException {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		List<Directory> list = new ArrayList<Directory>();
		try {
			Directory dir = new Directory();
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement(Query.selectDirectory());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				dir.setPathString(rs.getString(1));
				dir.setUsed(rs.getBoolean(2));
				dir.setRecusively(rs.getBoolean(3));
				list.add(dir);
			}
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (rs != null)
				rs.close();
			if (conn != null)
				conn.close();
		}
		return list;
	}

	public void dropDirectoryTable() throws SQLException {
		String query = Query.dropTable(TABLE_NAMES.DIRECTORY.name());
		Connection conn = null;

		try {
			conn = dataSource.getConnection();
			if (isExistDB(TABLE_NAMES.DIRECTORY.name())) {
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(query);
				}
			}
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	public void createDirectoryTable() throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			if (!isExistDB(TABLE_NAMES.DIRECTORY.name())) {
				conn = dataSource.getConnection();
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(Query.createTable(TABLE_NAMES.DIRECTORY.name()));
				}
			}
		} finally {
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}
	}
}
