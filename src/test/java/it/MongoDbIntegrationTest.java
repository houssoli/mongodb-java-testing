package it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

public class MongoDbIntegrationTest {
	private static final String BOOK_COLLECTION_NAME = "bookCollection";
	private static final String AUTHOR_1 = "Tim Tester";
	private static final String AUTHOR_2 = "Vaughn Vernon";
	private static final String BOOK1_TITLE = "Some book";
	private static final String BOOK2_TITLE = "Implementing Domain Driven Design";
	private static final String BOOK3_TITLE = "Anemic Domain Model and why I love it";

	MongodForTestsFactory factory;
	MongoClient mongo;

	@Before
	public void setup() throws Exception {
		factory = MongodForTestsFactory.with(Version.Main.PRODUCTION);
		mongo = factory.newMongo();
	}

	@After
	public void teardown() throws Exception {
		if (factory != null)
			factory.shutdown();
	}

	@Test
	public void shouldPersistAndFindBooks() {
		DB db = mongo.getDB("test-" + UUID.randomUUID());
		DBCollection collection = db.createCollection(BOOK_COLLECTION_NAME,
				new BasicDBObject());

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
}