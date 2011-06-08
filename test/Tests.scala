import play._
import play.test._

import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._

import models._
import play.db.anorm._

class BasicTests extends UnitFlatSpec with ShouldMatchers with BeforeAndAfterEach {

    // Delete all data before each test.
    override def beforeEach() {
        Fixtures.deleteDatabase()
    }
    
    it should "create and retrieve a Fragment" in {
        val body = """public class Foo {
            public static void main(String[] argv) {
                System.out.println("bar");
            }
        }"""

        Fragment.create(Fragment("A piece of Java code", body, "java"))
        Fragment.count().single() should be (1)

        val fragment = Fragment.find("style={style}").on("style" -> "java").first()

        fragment should not be (None)
        fragment.get.title should be ("A piece of Java code")
        fragment.get.body should be (body)
        fragment.get.style should be ("java")
    }

    /**
     * Roel's note: It seems that validations on Anorm 'Magic' models are
     * not honored.
     */
    it should "not create a Fragment without a title" in (pending)

    it should "only accept the given code styles" in {
        // as found in example code:
        val error = evaluating {
            Fragment.create(Fragment("Spam title", "Spammy content", "spam"))
        } should produce [IllegalArgumentException]        
        
        // other way of writing the same thing:
        intercept[IllegalArgumentException] {
            Fragment.create(Fragment("Spam title", "Spammy content", "spam"))
        }
    }

    it should "create and retrieve a Tag" in {
        models.Tag.create(models.Tag("dsl"))
        models.Tag.count().single() should be (1)
        
        val tag = models.Tag.find().first()
        
        tag should not be (None)
        tag.get.name should be ("dsl")
    }
    
    it should "create link between Fragments and Tags" in {
        // Create one tag with two tags:
        var fragment = Fragment.create(Fragment("A fragment", "int i=0;", "java")).get
        var tag1 = models.Tag.create(models.Tag("dsl")).get
        var tag2 = models.Tag.create(models.Tag("weird")).get
        
        fragment.addTag(tag1)
        fragment.addTag(tag2)
        
        Fragment.linkTags(fragment)
        
        // Retrieve the tag from the database:
        var fragmentWithTags = Fragment.findWithTags(fragment.id.get.get)
        
        // Assertions:
        fragmentWithTags.tags.length should be (2)
        fragmentWithTags.tags.map(t => t.name) should contain ("dsl")
        fragmentWithTags.tags.map(t => t.name) should contain ("weird")
    }

    it should "load Fragments without tags" in {
        var fragment = Fragment.create(Fragment("A fragment", "int i=0;", "java")).get
        var fragmentWithTags = Fragment.findWithTags(fragment.id.get.get)
        fragmentWithTags.tags.length should be (0)
    }
    
    it should "find an existing tag by name" in {
        var existingTag = models.Tag.create(models.Tag("performance")).get
        var tag = models.Tag.findOrCreate("performance")
        existingTag.id should be (tag.id)
    }
    
    it should "create a tag if can't be found" in {
        var tag = models.Tag.findOrCreate("performance")
        tag should not be (null)
        tag.id should not be (NotAssigned)
    }
    
    it should "find multiple Fragments with linked Tags" in {
        // Create two fragments with two tags each.
        createTwoFragmentsWithTwoTags();
        
        // Retrieve the saved fragments from the database.
        var fragmentsWithTags = Fragment.findAllWithTags()
        
        // Assertions:
        fragmentsWithTags.length should be (2)
        
        fragmentsWithTags.head.title should be ("A java fragment")
        fragmentsWithTags.head.tags.map(t => t.name) should contain ("basic")
        fragmentsWithTags.head.tags.map(t => t.name) should contain ("handy")
        
        fragmentsWithTags.last.title should be ("A ruby fragment")
        fragmentsWithTags.last.tags.map(t => t.name) should contain ("handy")
        fragmentsWithTags.last.tags.map(t => t.name) should contain ("math")
    }
 
    it should "find fragments by tag id" in {
        // Create two fragments with two tags each.
        createTwoFragmentsWithTwoTags();
 
        // Retrieve the saved fragments by tag id.
        var overlappingTag = models.Tag.findOrCreate("handy")
        var fragments = Fragment.findAllWithTagsByTagId(overlappingTag.id.get.get);
        
        fragments.length should be (2)
    }
 
    private def createTwoFragmentsWithTwoTags() = {
        var fragment1 = Fragment.create(Fragment("A java fragment", "int i=0;", "java")).get
        var fragment2 = Fragment.create(Fragment("A ruby fragment", "[1,2,3,4,5].collect { |n| n**n }", "ruby")).get
    
        var tag1 = models.Tag.create(models.Tag("basic")).get
        var tag2 = models.Tag.create(models.Tag("handy")).get
        var tag3 = models.Tag.create(models.Tag("math")).get
        
        fragment1.addTag(tag1)
        fragment1.addTag(tag2)
        Fragment.linkTags(fragment1);

        fragment2.addTag(tag2)
        fragment2.addTag(tag3)
        Fragment.linkTags(fragment2);
    }
}