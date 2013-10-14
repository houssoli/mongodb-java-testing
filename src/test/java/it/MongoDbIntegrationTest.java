package it;

import static org.junit.Assert.assertEquals;

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
	public void testPretechAddressPersistance() {
		DB db = mongo.getDB("test-" + UUID.randomUUID());
		DBCollection collection = db.createCollection("testCol",
				new BasicDBObject());
		BasicDBObject contact = new BasicDBObject();
		contact.put("name", "pretech");
		contact.put("address", "bangalore");
		// Inserting document
		collection.insert(contact);
		DBCursor cursorDoc = collection.find();
		BasicDBObject contact1 = new BasicDBObject();
		while (cursorDoc.hasNext()) {
			contact1 = (BasicDBObject) cursorDoc.next();
			System.out.println(contact1);
		}

		assertEquals(contact1.get("name"), "pretech1");

	}

}