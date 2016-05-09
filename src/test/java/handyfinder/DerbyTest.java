package handyfinder;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.junit.Test;

public class DerbyTest {
	
	Log LOG = LogFactory.getLog(DerbyTest.class);
	
	@Test
	public void connection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver);
		String protocol = "jdbc:derby:directory:/home/choechangwon/workspacejava/handyfinder/target/testdb;create=true";
		Connection con = DriverManager.getConnection(protocol);
		con.createStatement().execute("create table directory(pathString varchar(255), used BOOLEAN, recursively BOOLEAN)");
		ResultSet rs = con.createStatement().executeQuery("SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE='T'");
		assertTrue(rs.next());
		LOG.info(rs.getString(1).toString());
		rs.close();
		PreparedStatement pstmt = con.prepareStatement("drop table directory");
		pstmt.execute();
		rs = con.createStatement().executeQuery("SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE='T'");
		assertFalse(rs.next());
		rs.close();
	}
}
