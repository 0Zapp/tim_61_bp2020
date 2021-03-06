package database;

import database.settings.Settings;
//import lombok.AllArgsConstructor;
//import lombok.Data;
import resource.DBNode;
import resource.data.Row;
import resource.enums.AttributeType;
import resource.enums.ConstraintType;
import resource.implementation.Attribute;
import resource.implementation.AttributeConstraint;
import resource.implementation.Entity;
import resource.implementation.InformationResource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.StyledEditorKit.ForegroundAction;

//@Data
public class MSSQLrepository implements Repository {

	private Settings settings;
	private Connection connection;

	public MSSQLrepository(Settings settings) {
		this.settings = settings;
	}

	private void initConnection() throws SQLException, ClassNotFoundException {
		Class.forName("net.sourceforge.jtds.jdbc.Driver");
		String ip = (String) settings.getParameter("mssql_ip");
		String database = (String) settings.getParameter("mssql_database");
		String username = (String) settings.getParameter("mssql_username");
		String password = (String) settings.getParameter("mssql_password");
		Class.forName("net.sourceforge.jtds.jdbc.Driver");
		connection = DriverManager.getConnection("jdbc:jtds:sqlserver://" + ip + "/" + database, username, password);
	}

	private void closeConnection() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			connection = null;
		}
	}

	@Override
	public DBNode getSchema() {

		try {
			this.initConnection();

			DatabaseMetaData metaData = connection.getMetaData();
			InformationResource ir = new InformationResource("RAF_BP_Primer");

			String tableType[] = { "TABLE" };
			ResultSet tables = metaData.getTables(connection.getCatalog(), null, null, tableType);
			ArrayList<ResultSet> allfk = new ArrayList<ResultSet>();

			while (tables.next()) {

				String tableName = tables.getString("TABLE_NAME");
				Entity newTable = new Entity(tableName, ir);
				ir.addChild(newTable);

				// Koje atribute imaja ova tabela?

				ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, null);
				ResultSet foreignKeys = metaData.getImportedKeys(connection.getCatalog(), null, newTable.getName());
				allfk.add(foreignKeys);

				while (columns.next()) {

					ResultSet primaryKeys = metaData.getPrimaryKeys(connection.getCatalog(), null, newTable.getName());
					foreignKeys = metaData.getImportedKeys(connection.getCatalog(), null, newTable.getName());

					String columnName = columns.getString("COLUMN_NAME");
					String columnType = columns.getString("TYPE_NAME");
					int columnSize = Integer.parseInt(columns.getString("COLUMN_SIZE"));
					Attribute attribute = new Attribute(columnName, newTable,
							AttributeType.valueOf(columnType.toUpperCase()), columnSize);
					newTable.addChild(attribute);

					// detect if null
					String isNUll = columns.getString("IS_NULLABLE");
					if (isNUll.equals("NO")) {
						AttributeConstraint ac = new AttributeConstraint("NOT_NULL", attribute,
								ConstraintType.NOT_NULL);
						attribute.addChild(ac);
					}

					// detect if has default
					String hasDefault = columns.getString("COLUMN_DEF");
					if (hasDefault != null) {
						AttributeConstraint ac = new AttributeConstraint("DEFAULT_VALUE", attribute,
								ConstraintType.DEFAULT_VALUE);
						attribute.addChild(ac);
					}

					// detect if domain value
					String dataType = columns.getString("TYPE_NAME");
					boolean Domain = false;

					for (AttributeType a : AttributeType.values()) {
						if (dataType.toUpperCase().equals(a.name())) {
							Domain = true;
						}
					}

					if (!Domain) {
						AttributeConstraint ac = new AttributeConstraint("DOMAIN_VALUE", attribute,
								ConstraintType.DOMAIN_VALUE);
						attribute.addChild(ac);
					}

					// detect primary key
					while (primaryKeys.next()) {

						String pkColumnName = primaryKeys.getString("COLUMN_NAME");

						if (pkColumnName.equals(columnName)) {
							AttributeConstraint ac = new AttributeConstraint("PRIMARY_KEY", attribute,
									ConstraintType.PRIMARY_KEY);
							attribute.addChild(ac);
						}

					}

					// detect foreign keys
					while (foreignKeys.next()) {
						String TableName = foreignKeys.getString("FKTABLE_NAME");
						String ColumnName = foreignKeys.getString("FKCOLUMN_NAME");
						String pkTableName = foreignKeys.getString("PKTABLE_NAME");
						String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
						if (TableName.contentEquals(tableName) && ColumnName.equals(columnName)) {
							AttributeConstraint ac = new AttributeConstraint("FOREIGN_KEY", attribute,
									ConstraintType.FOREIGN_KEY);
							attribute.addChild(ac);

						}

						// private Attribute inRelationWith;
					}

				}

			}

			ArrayList<DBNode> entities = (ArrayList<DBNode>) ir.getChildren();

			for (ResultSet rs : allfk) {
				while (rs.next()) {
					String TableName = rs.getString("FKTABLE_NAME");
					String ColumnName = rs.getString("FKCOLUMN_NAME");
					String pkTableName = rs.getString("PKTABLE_NAME");
					String pkColumnName = rs.getString("PKCOLUMN_NAME");

					Entity table1 = null;
					Entity table2 = null;
					Attribute attribute1 = null;
					Attribute attribute2 = null;

					for (DBNode d : entities) {
						if (((Entity) d).getName().equals(TableName)) {
							table1 = (Entity) d;
							for (DBNode a : table1.getChildren()) {
								if (a.getName().equals(ColumnName)) {
									attribute1 = (Attribute) a;
								}
							}
						}
						if (((Entity) d).getName().equals(pkTableName)) {
							table2 = (Entity) d;
							for (DBNode a : table2.getChildren()) {
								if (a.getName().equals(pkColumnName)) {
									attribute2 = (Attribute) a;
								}
							}
						}
					}

					attribute1.setInRelationWith(attribute2);

				}
			}

			return ir;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.closeConnection();
		}

		return null;
	}

	@Override
	public void deleteRow(String[] data) {

		try {
			this.initConnection();

			String query = "DELETE FROM " + data[0] + " WHERE " + data[1] + "='" + data[data.length / 2 + 1] + "'";
			for (int i = 2; i <= data.length / 2; i++) {
				query += " AND " + data[i] + "='";
				query += data[data.length / 2 + i] + "'";
			}

			System.out.println(query);
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			preparedStatement.executeUpdate();

		} catch (Exception e) {
			System.out.println("upit se ne moze izvrsiti zbog konflikta");
		} finally {
			this.closeConnection();
		}

	}

	public void InsertRow(String[] data) {

		try {
			this.initConnection();

			String query = "INSERT INTO " + data[0] + "(" + data[1];
			for (int i = 2; i <= data.length / 2; i++) {
				query += ", " + data[i];
			}
			query += ") values ('" + data[data.length / 2 + 1];
			for (int i = data.length / 2 + 2; i < data.length; i++) {
				query += "','" + data[i];
			}
			query += "')";
			System.out.println(query);
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			preparedStatement.executeUpdate();

		} catch (Exception e) {
			System.out.println("upit se ne moze izvrsiti zbog konflikta");
		} finally {
			this.closeConnection();
		}

	}

	public void updateRow(String[] data, String[] original) {

		try {
			this.initConnection();

			String query = "UPDATE " + data[0] + " SET " + data[1] + "='" + data[data.length / 2 + 1] + "'";
			for (int i = 2; i <= data.length / 2; i++) {
				query += ", " + data[i] + "='";
				query += data[data.length / 2 + i] + "'";
			}
			query += " WHERE ";
			query += original[1] + "='" + original[original.length / 2 + 1] + "'";
			for (int i = 2; i <= original.length / 2; i++) {
				query += " AND " + original[i] + "='";
				query += original[original.length / 2 + i] + "'";
			}

			System.out.println(query);
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			preparedStatement.executeUpdate();

		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println("upit se ne moze izvrsiti zbog konflikta");
		} finally {
			this.closeConnection();
		}

	}

	@Override
	public List<Row> get(String from, String relatedAttr, String value) {
		//System.out.println("get");
		List<Row> rows = new ArrayList<>();
		//System.out.println(from+" "+relatedAttr+" "+value);

		try {
			this.initConnection();

			String query = "SELECT * FROM " + from;

			if (relatedAttr != null && value != null) {
				query+=" WHERE "+relatedAttr+"='"+value+"'";
			}
			
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				Row row = new Row();
				row.setName(from);

				ResultSetMetaData resultSetMetaData = rs.getMetaData();
				for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
					row.addField(resultSetMetaData.getColumnName(i), rs.getString(i));
				}
				rows.add(row);

			}

		} catch (Exception e) {
			System.out.print("selektovana tabela ne postoji");
		} finally {
			this.closeConnection();
		}

		return rows;
	}

	@Override
	public List<Row> FaS(String[] data, String[] filter, String[] sort) {

		List<Row> rows = new ArrayList<>();

		try {
			this.initConnection();

			String query = "SELECT";
			boolean first = true;

			for (int i = 1; i <= filter.length; i++) {
				if (filter[i - 1].equals("Prikazi")) {
					if (first) {
						query += " " + data[i];
						first = false;
					} else {
						query += ", " + data[i];
					}

				}
			}

			query += " FROM " + data[0];
			first = true;

			for (int i = 1; i <= sort.length; i++) {
				if (sort[i - 1].equals("Asc")) {
					if (first) {
						query += " ORDER BY";
						query += " " + data[i] + " ASC";
						first = false;
					} else {
						query += ", ";
						query += " " + data[i] + " ASC";
					}
				} else if (sort[i - 1].equals("Desc")) {
					if (first) {
						query += " ORDER BY ";
						query += " " + data[i] + " DESC";
						first = false;
					} else {
						query += ", ";
						query += " " + data[i] + " DESC";
					}
				}

			}

			System.out.println(query);
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				Row row = new Row();
				row.setName(data[0]);

				ResultSetMetaData resultSetMetaData = rs.getMetaData();
				for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
					row.addField(resultSetMetaData.getColumnName(i), rs.getString(i));
				}
				rows.add(row);

			}

		} catch (Exception e) {
			System.out.println("upit se ne moze izvrsiti zbog konflikta");
		} finally {
			this.closeConnection();
		}

		return rows;
	}

	@Override
	public List<Row> Search(String[] data) {

		List<Row> rows = new ArrayList<>();

		try {
			this.initConnection();

			String query = "SELECT * FROM " + data[0] + " WHERE " + data[1];
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				Row row = new Row();
				row.setName(data[0]);

				ResultSetMetaData resultSetMetaData = rs.getMetaData();
				for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
					row.addField(resultSetMetaData.getColumnName(i), rs.getString(i));
				}
				rows.add(row);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.closeConnection();
		}

		return rows;
	}

	@Override
	public List<Row> Report(String[] data) {
		List<Row> rows = new ArrayList<>();

		try {
			this.initConnection();

			String query = "SELECT " + data[1] + " FROM " + data[0] + data[2];
			System.out.println(query);
			PreparedStatement preparedStatement = connection.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				Row row = new Row();
				row.setName(data[0]);

				ResultSetMetaData resultSetMetaData = rs.getMetaData();
				for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
					row.addField(resultSetMetaData.getColumnName(i), rs.getString(i));
				}
				rows.add(row);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.closeConnection();
		}

		return rows;
	}
}
