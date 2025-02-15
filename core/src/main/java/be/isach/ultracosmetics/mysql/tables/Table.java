package be.isach.ultracosmetics.mysql.tables;

import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.type.CosmeticType;
import be.isach.ultracosmetics.mysql.query.InsertQuery;
import be.isach.ultracosmetics.mysql.query.InsertValue;
import be.isach.ultracosmetics.mysql.query.StandardQuery;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import javax.sql.DataSource;

public abstract class Table {
    private final DataSource dataSource;
    private final String name;
    protected final List<TableInfo> tableInfo = new ArrayList<>();

    public Table(DataSource dataSource, String name) {
        this.dataSource = dataSource;
        this.name = name;
    }

    public abstract void setupTableInfo();

    public String getCreateTableStatement() {
        StringJoiner infoJoiner = new StringJoiner(", ", "(", ")");
        for (TableInfo info : tableInfo) {
            infoJoiner.add(info.toSQL());
        }
        return "CREATE TABLE IF NOT EXISTS `" + name + "` " + infoJoiner.toString() + " ROW_FORMAT=DYNAMIC";
    }

    public void loadBaseData() {

    }

    public String getWrappedName() {
        return "`" + name + "`";
    }

    protected String getRawName() {
        return name;
    }

    public List<TableInfo> getTableInfo() {
        return Collections.unmodifiableList(tableInfo);
    }

    public StandardQuery select(String columns) {
        return new StandardQuery(this, "SELECT " + columns + " FROM");
    }

    public StandardQuery selectVoid() {
        return select("1");
    }

    public StandardQuery update() {
        return new StandardQuery(this, "UPDATE");
    }

    public StandardQuery delete() {
        return new StandardQuery(this, "DELETE FROM");
    }

    public InsertQuery insert(String... columns) {
        return new InsertQuery(this, columns);
    }

    public InsertQuery insertIgnore(String... columns) {
        return new InsertQuery(this, true, columns);
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String cleanCosmeticName(CosmeticType<?> cosmetic) {
        return cosmetic == null ? null : cosmetic.getConfigName().toLowerCase();
    }

    public static String cleanCategoryName(Category cat) {
        if (cat == null) return null;
        return cat.toString().toLowerCase();
    }

    public static String cleanCategoryName(CosmeticType<?> cosmetic) {
        return cleanCategoryName(cosmetic.getCategory());
    }

    public static byte[] binaryUUID(UUID uuid) {
        return hexStringToByteArray(uuid.toString().replace("-", ""));
    }

    public static InsertValue insertUUID(UUID uuid) {
        return new InsertValue(binaryUUID(uuid));
    }

    /**
     * https://stackoverflow.com/a/140861
     *
     * @param s Hex to convert to bytes
     * @return byte array
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
