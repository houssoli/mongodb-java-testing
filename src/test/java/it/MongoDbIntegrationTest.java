package it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
	private static final String BOOK2_AUTHOR = "Vaughn Vernon";
	private static final String BOOK2_TITLE = "Implementing Domain Driven Design";
	private static final String BOOK1_AUTHOR = "Tim Tester";
	private static final String BOOK1_TITLE = "Some book";

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
		DBCollection collection = db.createCollection("bookCollection",
				new BasicDBObject());
		BasicDBObject book1 = new BasicDBObject();
		book1.put("title", BOOK1_TITLE);
		book1.put("author", BOOK1_AUTHOR);

		BasicDBObject book2 = new BasicDBObject();
		book2.put("title", BOOK2_TITLE);
		book2.put("author", BOOK2_AUTHOR);

		collection.insert(book1);
		collection.insert(book2);

		DBCursor docCursor = collection.find();
		assertThat(docCursor.size(), equalTo(2));

		BasicDBObject bookFromDb1 = (BasicDBObject) docCursor.next();
		assertThat((String) bookFromDb1.get("title"), equalTo(BOOK1_TITLE));
		assertThat((String) bookFromDb1.get("author"), equalTo(BOOK1_AUTHOR));

		BasicDBObject bookFromDb2 = (BasicDBObject) docCursor.next();
		assertThat((String) bookFromDb2.get("title"), equalTo(BOOK2_TITLE));
		assertThat((String) bookFromDb2.get("author"), equalTo(BOOK2_AUTHOR));
	}

}