package handyfinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.junit.Test;

public class DerbyTest {
	
	Log LOG = LogFactory.getLog(DerbyTest.class);
	
	@Test
	public void connection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, URISyntaxException, IOException {
		Path path = Paths.get(getClass().getResource("/").toURI());
		path = path.resolve("test-db");
		if(Files.exists(path)) FileUtils.deleteDirectory(path.toFile());
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver);
		String protocol = "jdbc:derby:directory:" + path.toAbsolutePath().toString() + ";create=true";
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
