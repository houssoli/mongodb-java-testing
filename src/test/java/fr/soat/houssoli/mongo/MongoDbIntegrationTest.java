package fr.soat.houssoli.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

import fr.soat.houssoli.mongo.utils.MongoDBUtilsTestsFactory;

public class MongoDbIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbIntegrationTest.class);

    private static final String BOOK_COLLECTION_NAME = "bookCollection";
    private static final String AUTHOR_1 = "Tim Tester";
    private static final String AUTHOR_2 = "Vaughn Vernon";
    private static final String BOOK1_TITLE = "Some book";
    private static final String BOOK2_TITLE = "Implementing Domain Driven Design";
    private static final String BOOK3_TITLE = "Anemic Domain Model and why I love it";

    // admin
    private static final String userAdmin = "siteUserAdmin";
    private static final String passwordAdmin = "password";
    private static final Object[] roleUserAdminAnyDatabase = new Object[] { "userAdminAnyDatabase" };
    private static final String adminDatabase = "admin";

    // bus mongodb main user
    private static final String busUserName = "mongodbBusUsername";
    private static final String busUserPassword = "mongodbBusPassword";
    private static final Object[] busUserRole = new Object[] { "readWrite", "dbAdmin" };
    private static final String busDatabase = "mongodbbusesb";
    private static final String busCollectionNameAom = "actsofmanagement";
    private static final String busCollectionNameRpt = "report";

    // bus mongodb reporting user
    private static final String busReportUserName = "mongodbReportUsername";
    private static final String busReportUserPassword = "mongodbReportPassword";

    private static MongoDBUtilsTestsFactory factory;
    private MongoClient mongo;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        if (factory == null) {
            factory = MongoDBUtilsTestsFactory.with(MongoDBUtilsTestsFactory.VersionEnum.V2_4_14);

            // ///////////////////////////////////////////////////
            populateUserAdminDatabase();

            // //////////////////////////////////////////////////////////
            populateBusesbUserBusDatabase();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (factory != null) {
            factory.shutdown();
        }
    }

    @Before
    public void setup() throws Exception {
        // factory = de.flapdoodle.embed.mongo.tests.MongodForTestsFactory.with(Version.Main.PRODUCTION)
        // mongo = factory.newMongo();

        setUpMongo();
    }

    @After
    public void teardown() throws Exception {
        closeMongoClient(mongo);
    }

    private void setUpMongo() throws IOException, UnknownHostException, MongoException, MongoException {
        mongo = factory.createMongoClient();
    }

    private static void populateUserAdminDatabase() throws UnknownHostException {
        MongoClient mongoClient = factory.createMongoClient();

        // ///////////////////////////////////////////////////
        DB mongoGetDBAdmin = mongoClient.getDB(adminDatabase);
        LOGGER.debug("mongoGetDBAdmin => {}", mongoGetDBAdmin);
        // create Administrator
        // DBObject lookupUserAdmin = findOneUser(mongoGetDBAdmin, userAdmin);
        // if (lookupUserAdmin == null) { // check if exists before creation of Administrator
        DBObject dboCreateAdministrator = new BasicDBObject("user", userAdmin).append("pwd", passwordAdmin).append("roles",
                roleUserAdminAnyDatabase);
        String cmdCreateAdministrator = String.format("db.addUser(%s)", dboCreateAdministrator);
        LOGGER.debug("createAdministrator Request => {}", cmdCreateAdministrator);
        // Object createAdministratorCommandResult = mongoGetDBAdmin.eval(cmdCreateAdministrator, new Object[] {});

        Object createAdministratorCommandResult = mongoGetDBAdmin.addUser(userAdmin, passwordAdmin.toCharArray());
        LOGGER.debug("admin, db.addUser() commandResult => {}", createAdministratorCommandResult);
        // }

        // authenticate Administrator
        Object adminAuthenticateCommandResult = mongoGetDBAdmin.authenticateCommand("siteUserAdmin", "password".toCharArray());
        LOGGER.debug("adminAuthenticateCommandResult => {}", adminAuthenticateCommandResult);

        // lookup the users in the administrator database
        DBObject cmdGetUserAdminResult = findOneUser(mongoGetDBAdmin, userAdmin);
        assertThat((String) cmdGetUserAdminResult.get("user"), equalTo(userAdmin));

        Iterator<DBObject> adminSet = findUsers(mongoGetDBAdmin);
        if (adminSet.hasNext()) {
            DBObject obj = adminSet.next();
            LOGGER.debug("{}, user['{}'] ==========> {}", mongoGetDBAdmin.getName(), obj.get("user"), obj);
        }

        mongoClient.close();
    }

    private static void populateBusesbUserBusDatabase() throws UnknownHostException {
        List<MongoCredential> credentialList = Arrays.asList(new MongoCredential[] { MongoCredential.createMongoCRCredential(userAdmin,
                adminDatabase, passwordAdmin.toCharArray()) });
        MongoClient mongoClient = factory.createMongoClient(credentialList);

        DB mongoGetBusDatabase = mongoClient.getDB(busDatabase);
        LOGGER.debug("mongoGetBusDatabase => {}", mongoGetBusDatabase);
        // ////////////////////////////////////////////////////
        {
            // DBObject lookupBusUserExists = findOneUser(mongoGetBusDatabase, busUserName);
            // if (lookupBusUserExists == null) {// check if exists before creation of busUser
            WriteResult createBusUserCommandResult = mongoGetBusDatabase.addUser(busUserName, busUserPassword.toCharArray());
            LOGGER.debug("{}, db.addUser() commandResult => {}", mongoGetBusDatabase, createBusUserCommandResult);
            // }

            DBObject cmdGetUserBusResult = findOneUser(mongoGetBusDatabase, busUserName);
            assertThat((String) cmdGetUserBusResult.get("user"), equalTo(busUserName));

            Iterator<DBObject> resultSetCursor = findUsers(mongoGetBusDatabase);
            if (resultSetCursor.hasNext()) {
                DBObject obj = resultSetCursor.next();
                LOGGER.debug("{}, user['{}'] ==========> {}", mongoGetBusDatabase.getName(), obj.get("user"), obj);
            }
        }
        mongoClient.close();
    }

    private void populateReportUserBusDatabase() throws UnknownHostException {
        MongoClient mongoClient = factory.createMongoClient();
        DB mongoGetBusDatabase = mongoClient.getDB(busDatabase);
        Object userBusAuthenticateCommandResult = mongoGetBusDatabase.authenticateCommand(busUserName, busUserPassword.toCharArray());
        LOGGER.debug("userBusAuthenticateCommandResult => {}", userBusAuthenticateCommandResult);
        // make the BusUser to add a BusReportUser
        if (mongoGetBusDatabase.authenticate(busUserName, busUserPassword.toCharArray())) {
            WriteResult addUserWriteResult = mongoGetBusDatabase.addUser(busReportUserName, busReportUserPassword.toCharArray());
        }
        mongoClient.close();
    }

    private void populateSomeReportData() throws UnknownHostException {
        DBCollection collection = null;
        Set<String> collectionNames = null;

        MongoClient mongoClient = factory.createMongoClient();
        DB db = mongoClient.getDB(busDatabase);
        try {
            CommandResult authenticateUserReportCommand = db.authenticateCommand(busReportUserName, busReportUserPassword.toCharArray());
            LOGGER.debug("authenticateUserReportCommand => {}", authenticateUserReportCommand);

            collection = db.collectionExists(busCollectionNameRpt) ? db.getCollection(busCollectionNameRpt) : db.createCollection(
                    busCollectionNameRpt, new BasicDBObject());
            collectionNames = db.getCollectionNames();

            assertThat(collectionNames, hasItem(busCollectionNameRpt));
            assertThat(collection.find().size(), equalTo(0));
        } finally {
            mongoClient.close();
        }
    }

    private static Iterator<DBObject> findUsers(final DB db) {
        DBCursor resultSetCursor = db.getCollection("system.users").find();
        LOGGER.debug("{}, db.system.users.find() => {}", db.getName(), resultSetCursor);
        int i = 0;
        if (resultSetCursor.hasNext()) {
            DBObject obj = resultSetCursor.next();
            LOGGER.debug("{}, user[{}] => {}", db.getName(), i++, obj);
        }
        return resultSetCursor.iterator();
    }

    private static DBObject findOneUser(final DB db, final String username) {
        DBObject criteria = new BasicDBObject("user", username);
        DBObject result = db.getCollection("system.users").findOne(criteria);
        LOGGER.debug("{}, db.system.users.findOne({}) => {}", db.getName(), criteria, result);
        return result;
    }

    private static void closeMongoClient(MongoClient mongoClient) {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    public void shouldInsertReports() throws UnknownHostException {
        // //////////////////////////////////////////////////////////
        populateReportUserBusDatabase();

        // //////////////////////////////////////////////////////////
        // authenticate the BusReportUser in order to write some reports
        populateSomeReportData();
    }

    @Test
    public void shouldPersistAndFindBooks() {

        DB db = mongo.getDB("test-" + UUID.randomUUID());
        DBCollection collection = db.createCollection(BOOK_COLLECTION_NAME, new BasicDBObject());

        Set<String> collectionNames = db.getCollectionNames();
        assertThat(collectionNames, hasItem(BOOK_COLLECTION_NAME));

        BasicDBObject book1 = new BasicDBObject();
        book1.put("title", BOOK1_TITLE);
        book1.put("author", AUTHOR_1);

        BasicDBObject book2 = new BasicDBObject();
        book2.put("title", BOOK2_TITLE);
        book2.put("author", AUTHOR_2);

        BasicDBObject book3 = new BasicDBObject();
        book3.put("title", BOOK3_TITLE);
        book3.put("author", AUTHOR_1);

        collection.insert(book1);
        collection.insert(book2);
        collection.insert(book3);

        assertThat(collection.find().size(), equalTo(3));

        BasicDBObject query1 = new BasicDBObject("title", BOOK1_TITLE);
        BasicDBObject bookFromDb1 = (BasicDBObject) collection.findOne(query1);
        assertThat((String) bookFromDb1.get("title"), equalTo(BOOK1_TITLE));
        assertThat((String) bookFromDb1.get("author"), equalTo(AUTHOR_1));

        BasicDBObject query2 = new BasicDBObject("author", AUTHOR_1);
        DBCursor docCursor = collection.find(query2);
        assertThat(docCursor.size(), equalTo(2));

        BasicDBObject bookByAuthor1 = (BasicDBObject) docCursor.next();
        assertThat((String) bookByAuthor1.get("title"), equalTo(BOOK1_TITLE));
        assertThat((String) bookByAuthor1.get("author"), equalTo(AUTHOR_1));

        BasicDBObject bookByAuthor2 = (BasicDBObject) docCursor.next();
        assertThat((String) bookByAuthor2.get("title"), equalTo(BOOK3_TITLE));
        assertThat((String) bookByAuthor2.get("author"), equalTo(AUTHOR_1));
    }

    @Test
    public void shouldCountCategoriesUsingMapReduce() throws Exception {
        DB db = mongo.getDB("test-" + UUID.randomUUID());
        DBCollection collection = db.createCollection(BOOK_COLLECTION_NAME, new BasicDBObject());
        BasicDBObject book1 = new BasicDBObject().append("title", BOOK1_TITLE).append("author", AUTHOR_1)
                .append("categories", new String[] { "crime", "horror", "mystery" });

        BasicDBObject book2 = new BasicDBObject().append("title", BOOK2_TITLE).append("author", AUTHOR_2)
                .append("categories", new String[] { "science", "mystery", "sports" });

        BasicDBObject book3 = new BasicDBObject().append("title", BOOK3_TITLE).append("author", AUTHOR_1)
                .append("categories", new String[] { "horror", "science", "romance" });

        collection.insert(book1);
        collection.insert(book2);
        collection.insert(book3);

        // java please buy some groovy-style multiline support ''' :\
        String map = "function(){" + "this.categories.forEach(" + "function(category){emit(category, {count:1});}" + ");" + "};";
        String reduce = "function(key, values){" + "var sum = 0;" + "for(var i=0;i<values.length;i++)" + "sum += values[i].count;"
                + "return {count: sum};" + "};";
        MapReduceOutput output = collection.mapReduce(map, reduce, null, MapReduceCommand.OutputType.INLINE, null);
        List<DBObject> result = Lists.newArrayList(output.results());
        Collections.sort(result, bookComparator);

        assertThat((String) result.get(0).get("_id"), equalTo("crime"));
        DBObject count1 = (DBObject) result.get(0).get("value");
        assertThat((Double) count1.get("count"), equalTo(1.0D));

        assertThat((String) result.get(1).get("_id"), equalTo("horror"));
        DBObject count2 = (DBObject) result.get(1).get("value");
        assertThat((Double) count2.get("count"), equalTo(2.0D));

        assertThat((String) result.get(2).get("_id"), equalTo("mystery"));
        DBObject count3 = (DBObject) result.get(2).get("value");
        assertThat((Double) count3.get("count"), equalTo(2.0D));

        // [..]
    }

    static Comparator<DBObject> bookComparator = new Comparator<DBObject>() {
        @Override
        public int compare(final DBObject o1, final DBObject o2) {
            return ((String) o1.get("_id")).compareTo((String) o2.get("_id"));
        }
    };
}