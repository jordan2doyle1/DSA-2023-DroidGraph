package phd.research;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

public class StringTableTest {

    String[][] tableData;

    @Before
    public void setUp() {
        this.tableData = new String[][]{{"id", "First Name", "Last Name", "Age"}, {"1", "John", "Johnson", "45"},
                {"2", "Tom", "", "35"}, {"3", "Rose", "Johnson", "22"}, {"4", "Jimmy", "Kimmel", ""}};
    }

    @Test
    public void testSimpleTable() {
        String expectedRightJustifiedTable =
                "| id | First Name | Last Name | Age |\n" + "|  1 |       John |   Johnson |  45 |\n" +
                        "|  2 |        Tom |           |  35 |\n" + "|  3 |       Rose |   Johnson |  22 |\n" +
                        "|  4 |      Jimmy |    Kimmel |     |\n";
        assertEquals("Right justified simple table formatted wrong.", expectedRightJustifiedTable,
                StringTable.simpleTable(this.tableData, false)
                    );

        String expectedLeftJustifiedTable =
                "| id | First Name | Last Name | Age |\n" + "| 1  | John       | Johnson   | 45  |\n" +
                        "| 2  | Tom        |           | 35  |\n" + "| 3  | Rose       | Johnson   | 22  |\n" +
                        "| 4  | Jimmy      | Kimmel    |     |\n";
        assertEquals("Left justified simple table formatted wrong.", expectedLeftJustifiedTable,
                StringTable.simpleTable(this.tableData, true)
                    );
    }

    @Test
    public void testTableWithLines() {
        String expectedRightJustifiedTable =
                "+----+------------+-----------+-----+\n" + "| id | First Name | Last Name | Age |\n" +
                        "+----+------------+-----------+-----+\n" + "|  1 |       John |   Johnson |  45 |\n" +
                        "|  2 |        Tom |           |  35 |\n" + "|  3 |       Rose |   Johnson |  22 |\n" +
                        "|  4 |      Jimmy |    Kimmel |     |\n" + "+----+------------+-----------+-----+\n";
        assertEquals("Right justified table with lines formatted wrong.", expectedRightJustifiedTable,
                StringTable.tableWithLines(this.tableData, false)
                    );

        String expectedLeftJustifiedTable =
                "+----+------------+-----------+-----+\n" + "| id | First Name | Last Name | Age |\n" +
                        "+----+------------+-----------+-----+\n" + "| 1  | John       | Johnson   | 45  |\n" +
                        "| 2  | Tom        |           | 35  |\n" + "| 3  | Rose       | Johnson   | 22  |\n" +
                        "| 4  | Jimmy      | Kimmel    |     |\n" + "+----+------------+-----------+-----+\n";
        assertEquals("Left justified table with lines formatted wrong.", expectedLeftJustifiedTable,
                StringTable.tableWithLines(this.tableData, true)
                    );
    }

    @Test
    public void testTableWithLinesAndMaxWidth() {
        String[][] tableData = new String[][]{{"id", "First Name", "Last Name", "Age", "Profile"},
                {"1", "John", "Johnson", "45", "My name is John Johnson. My id is 1. My age is 45."},
                {"2", "Tom", "", "35", "My name is Tom. My id is 2. My age is 35."},
                {"3", "Rose", "Johnson", "22", "My name is Rose Johnson. My id is 3. My age is 22."},
                {"4", "Jimmy", "Kimmel", "", "My name is Jimmy Kimmel. My id is 4. My age is not specified. I am the" +
                        " host of the late night show. I am not fan of Matt Damon."}};

        String expectedRightJustifiedTable =
                "+----+------------+-----------+-----+--------------------------------+\n" +
                        "| id | First Name | Last Name | Age |                        Profile |\n" +
                        "+----+------------+-----------+-----+--------------------------------+\n" +
                        "|  1 |       John |   Johnson |  45 | My name is John Johnson. My id |\n" +
                        "|    |            |           |     |            is 1. My age is 45. |\n" +
                        "|    |            |           |     |                                |\n" +
                        "|  2 |        Tom |           |  35 | My name is Tom. My id is 2. My |\n" +
                        "|    |            |           |     |                     age is 35. |\n" +
                        "|    |            |           |     |                                |\n" +
                        "|  3 |       Rose |   Johnson |  22 | My name is Rose Johnson. My id |\n" +
                        "|    |            |           |     |            is 3. My age is 22. |\n" +
                        "|    |            |           |     |                                |\n" +
                        "|  4 |      Jimmy |    Kimmel |     | My name is Jimmy Kimmel. My id |\n" +
                        "|    |            |           |     |  is 4. My age is not specified |\n" +
                        "|    |            |           |     | . I am the host of the late ni |\n" +
                        "|    |            |           |     | ght show. I am not fan of Matt |\n" +
                        "|    |            |           |     |                         Damon. |\n" +
                        "|    |            |           |     |                                |\n" +
                        "+----+------------+-----------+-----+--------------------------------+\n";
        assertEquals("Right justified table with max width formatted wrong.", expectedRightJustifiedTable,
                StringTable.tableWithLinesAndMaxWidth(tableData, false, 30)
                    );

        String expectedLeftJustifiedTable = "+----+------------+-----------+-----+--------------------------------+\n" +
                "| id | First Name | Last Name | Age | Profile                        |\n" +
                "+----+------------+-----------+-----+--------------------------------+\n" +
                "| 1  | John       | Johnson   | 45  | My name is John Johnson. My id |\n" +
                "|    |            |           |     |  is 1. My age is 45.           |\n" +
                "|    |            |           |     |                                |\n" +
                "| 2  | Tom        |           | 35  | My name is Tom. My id is 2. My |\n" +
                "|    |            |           |     |  age is 35.                    |\n" +
                "|    |            |           |     |                                |\n" +
                "| 3  | Rose       | Johnson   | 22  | My name is Rose Johnson. My id |\n" +
                "|    |            |           |     |  is 3. My age is 22.           |\n" +
                "|    |            |           |     |                                |\n" +
                "| 4  | Jimmy      | Kimmel    |     | My name is Jimmy Kimmel. My id |\n" +
                "|    |            |           |     |  is 4. My age is not specified |\n" +
                "|    |            |           |     | . I am the host of the late ni |\n" +
                "|    |            |           |     | ght show. I am not fan of Matt |\n" +
                "|    |            |           |     |  Damon.                        |\n" +
                "|    |            |           |     |                                |\n" +
                "+----+------------+-----------+-----+--------------------------------+\n";
        assertEquals("Left justified table with max width formatted wrong.", expectedLeftJustifiedTable,
                StringTable.tableWithLinesAndMaxWidth(tableData, true, 30)
                    );
    }
}