/******************************************************************************
 * EvaluationDaoImplTest.java - created by aaronz@vt.edu
 * 
 * Copyright (c) 2007 Virginia Polytechnic Institute and State University
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 * 
 * Contributors:
 * Aaron Zeckoski (aaronz@vt.edu) - primary
 * 
 *****************************************************************************/

package org.sakaiproject.evaluation.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.evaluation.model.EvalAnswer;
import org.sakaiproject.evaluation.model.EvalEvaluation;
import org.sakaiproject.evaluation.model.EvalItem;
import org.sakaiproject.evaluation.model.EvalLock;
import org.sakaiproject.evaluation.model.EvalScale;
import org.sakaiproject.evaluation.model.EvalTemplate;
import org.sakaiproject.evaluation.model.EvalTemplateItem;
import org.sakaiproject.evaluation.model.constant.EvalConstants;
import org.sakaiproject.evaluation.test.EvalTestDataLoad;
import org.sakaiproject.evaluation.test.PreloadTestDataImpl;
import org.springframework.test.AbstractTransactionalSpringContextTests;


/**
 * Testing for the Evaluation Data Access Layer
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class EvaluationDaoImplTest extends AbstractTransactionalSpringContextTests {

   protected EvaluationDao evaluationDao;

   private EvalTestDataLoad etdl;

   private EvalScale scaleLocked;
   private EvalItem itemLocked;
   private EvalItem itemUnlocked;
   private EvalEvaluation evalUnLocked;

   protected String[] getConfigLocations() {
      // point to the needed spring config files, must be on the classpath
      // (add component/src/webapp/WEB-INF to the build path in Eclipse),
      // they also need to be referenced in the project.xml file
      return new String[] {"hibernate-test.xml", "spring-hibernate.xml"};
   }

   // run this before each test starts
   protected void onSetUpBeforeTransaction() throws Exception {
      // load the spring created dao class bean from the Spring Application Context
      evaluationDao = (EvaluationDao) applicationContext.getBean("org.sakaiproject.evaluation.dao.EvaluationDao");
      if (evaluationDao == null) {
         throw new NullPointerException("DAO could not be retrieved from spring evalGroupId");
      }

      // check the preloaded data
      assertTrue("Error preloading data", evaluationDao.countAll(EvalScale.class) > 0);

      // check the preloaded test data
      assertTrue("Error preloading test data", evaluationDao.countAll(EvalEvaluation.class) > 0);

      PreloadTestDataImpl ptd = (PreloadTestDataImpl) applicationContext.getBean("org.sakaiproject.evaluation.test.PreloadTestData");
      if (ptd == null) {
         throw new NullPointerException("PreloadTestDataImpl could not be retrieved from spring evalGroupId");
      }

      // get test objects
      etdl = ptd.getEtdl();

      // init the test class if needed

   }

   // run this before each test starts and as part of the transaction
   protected void onSetUpInTransaction() {
      // preload additional data if desired
      String[] optionsA = {"Male", "Female", "Unknown"};
      scaleLocked = new EvalScale(new Date(), EvalTestDataLoad.ADMIN_USER_ID, "Scale Alpha", 
            EvalConstants.SCALE_MODE_SCALE, EvalConstants.SHARING_PRIVATE, EvalTestDataLoad.NOT_EXPERT, 
            "description", EvalConstants.SCALE_IDEAL_NONE, optionsA, EvalTestDataLoad.LOCKED);
      evaluationDao.save( scaleLocked );

      itemLocked = new EvalItem(new Date(), EvalTestDataLoad.MAINT_USER_ID, "Header type locked", 
            EvalConstants.SHARING_PRIVATE, EvalConstants.ITEM_TYPE_HEADER, EvalTestDataLoad.NOT_EXPERT);
      itemLocked.setLocked(EvalTestDataLoad.LOCKED);
      evaluationDao.save( itemLocked );

      itemUnlocked = new EvalItem(new Date(), EvalTestDataLoad.MAINT_USER_ID, "Header type locked", 
            EvalConstants.SHARING_PRIVATE, EvalConstants.ITEM_TYPE_HEADER, EvalTestDataLoad.NOT_EXPERT);
      itemUnlocked.setScale(etdl.scale2);
      itemUnlocked.setScaleDisplaySetting( EvalConstants.ITEM_SCALE_DISPLAY_VERTICAL );
      itemUnlocked.setCategory(EvalConstants.ITEM_CATEGORY_COURSE);
      itemUnlocked.setLocked(EvalTestDataLoad.UNLOCKED);
      evaluationDao.save( itemUnlocked );

      evalUnLocked = new EvalEvaluation(new Date(), EvalTestDataLoad.MAINT_USER_ID, "Eval active not taken", null, 
            etdl.yesterday, etdl.tomorrow, etdl.tomorrow, etdl.threeDaysFuture, null, null,
            EvalConstants.EVALUATION_STATE_ACTIVE, EvalConstants.INSTRUCTOR_OPT_IN, 
            new Integer(1), null, null, null, null, etdl.templatePublicUnused, null, null,
            Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, EvalTestDataLoad.UNLOCKED,
            EvalConstants.EVALUATION_AUTHCONTROL_AUTH_REQ, null);
      evaluationDao.save( evalUnLocked );

   }

   /**
    * ADD unit tests below here, use testMethod as the name of the unit test,
    * Note that if a method is overloaded you should include the arguments in the
    * test name like so: testMethodClassInt (for method(Class, int);
    */


   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#getVisibleTemplates(java.lang.String, boolean, boolean, boolean)}.
    */
   public void testGetVisibleTemplates() {
      List<EvalTemplate> l = null;
      List<Long> ids = null;

      // all templates visible to user
      l = evaluationDao.getVisibleTemplates(EvalTestDataLoad.USER_ID, 
            new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertNotNull(l);
      assertEquals(5, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templatePublic.getId() ));
      assertTrue(ids.contains( etdl.templatePublicUnused.getId() ));
      assertTrue(ids.contains( etdl.templateUser.getId() ));
      assertTrue(ids.contains( etdl.templateUserUnused.getId() ));
      assertTrue(ids.contains( etdl.templateEid.getId() ));

      // all templates visible to maint user
      l = evaluationDao.getVisibleTemplates(EvalTestDataLoad.MAINT_USER_ID,
            new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertNotNull(l);
      assertEquals(4, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templatePublic.getId() ));
      assertTrue(ids.contains( etdl.templatePublicUnused.getId() ));
      assertTrue(ids.contains( etdl.templateUnused.getId() ));
      assertTrue(ids.contains( etdl.templateEid.getId() ));

      // all templates owned by USER
      l = evaluationDao.getVisibleTemplates(EvalTestDataLoad.USER_ID,
            new String[] {}, true);
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateUser.getId() ));
      assertTrue(ids.contains( etdl.templateUserUnused.getId() ));

      // all private templates
      l = evaluationDao.getVisibleTemplates(null,
            new String[] {}, true);
      assertNotNull(l);
      assertEquals(6, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateAdmin.getId() ));
      assertTrue(ids.contains( etdl.templateAdminNoItems.getId() ));
      assertTrue(ids.contains( etdl.templateUnused.getId() ));
      assertTrue(ids.contains( etdl.templateUser.getId() ));
      assertTrue(ids.contains( etdl.templateUserUnused.getId() ));
      assertTrue(ids.contains( etdl.templateAdminBlock.getId() ));

      // all private non-empty templates
      l = evaluationDao.getVisibleTemplates(null,
            new String[] {}, false);
      assertNotNull(l);
      assertEquals(5, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateAdmin.getId() ));
      assertTrue(ids.contains( etdl.templateUnused.getId() ));
      assertTrue(ids.contains( etdl.templateUser.getId() ));
      assertTrue(ids.contains( etdl.templateUserUnused.getId() ));
      assertTrue(ids.contains( etdl.templateAdminBlock.getId() ));

      // all public templates
      l = evaluationDao.getVisibleTemplates("", 
            new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templatePublic.getId() ));
      assertTrue(ids.contains( etdl.templatePublicUnused.getId() ));
      assertTrue(ids.contains( etdl.templateEid.getId() ));

      // all templates (admin would use this)
      l = evaluationDao.getVisibleTemplates(null, 
            new String[] {EvalConstants.SHARING_PUBLIC, EvalConstants.SHARING_SHARED, EvalConstants.SHARING_VISIBLE}, true);
      assertNotNull(l);
      assertEquals(9, l.size());

      // all non-empty templates (admin would use this)
      l = evaluationDao.getVisibleTemplates(null, 
            new String[] {EvalConstants.SHARING_PUBLIC, EvalConstants.SHARING_SHARED, EvalConstants.SHARING_VISIBLE}, false);
      assertNotNull(l);
      assertEquals(8, l.size());

      // no templates (no one should do this, it throws an exception)
      l = evaluationDao.getVisibleTemplates("", new String[] {}, true);
      assertNotNull(l);
      assertEquals(0, l.size());
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#countVisibleTemplates(java.lang.String, boolean, boolean, boolean)}.
    */
   public void testCountVisibleTemplates() {
      // all templates visible to user
      int count = evaluationDao.countVisibleTemplates(EvalTestDataLoad.USER_ID, 
            new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertEquals(5, count);

      // all templates visible to maint user
      count = evaluationDao.countVisibleTemplates(EvalTestDataLoad.MAINT_USER_ID, 
            new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertEquals(4, count);

      // all templates owned by USER
      count = evaluationDao.countVisibleTemplates(EvalTestDataLoad.USER_ID, 
            new String[] {}, true);
      assertEquals(2, count);

      // all private templates (admin only)
      count = evaluationDao.countVisibleTemplates(null, 
            new String[] {}, true);
      assertEquals(6, count);

      // all private non-empty templates (admin only)
      count = evaluationDao.countVisibleTemplates(null, 
            new String[] {}, false);
      assertEquals(5, count);

      // all public templates
      count = evaluationDao.countVisibleTemplates("", new String[] {EvalConstants.SHARING_PUBLIC}, true);
      assertEquals(3, count);

      // all templates (admin would use this)
      count = evaluationDao.countVisibleTemplates(null, 
            new String[] {EvalConstants.SHARING_PUBLIC, EvalConstants.SHARING_SHARED, EvalConstants.SHARING_VISIBLE}, true);
      assertEquals(9, count);

      // all non-empty templates (admin would use this)
      count = evaluationDao.countVisibleTemplates(null, 
            new String[] {EvalConstants.SHARING_PUBLIC, EvalConstants.SHARING_SHARED, EvalConstants.SHARING_VISIBLE}, false);
      assertEquals(8, count);
   }

   public void testGetEvaluationsByEvalGroups() {
      List<EvalEvaluation> l = null;
      List<Long> ids = null;

      // test getting evaluationSetupService for 2 sites
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF}, false, true, false);
      assertNotNull(l);
      assertEquals(4, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationNewAdmin.getId() ));
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationClosed.getId() ));
      assertTrue(ids.contains( etdl.evaluationViewable.getId() ));

      // test getting evaluationSetupService by evalGroupId
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE1_REF}, false, true, false);
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationNewAdmin.getId() ));
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationClosed.getId() ));

      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE2_REF}, false, true, false);
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(! ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationNewAdmin.getId() ));
      assertTrue(ids.contains( etdl.evaluationClosed.getId() ));
      assertTrue(ids.contains( etdl.evaluationViewable.getId() ));

      // test getting by groupId and getting anons
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE1_REF}, false, true, true);
      assertNotNull(l);
      assertEquals(4, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationNewAdmin.getId() ));
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationActiveUntaken.getId() ));
      assertTrue(ids.contains( etdl.evaluationClosed.getId() ));
      
      // test invalid site
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {"invalid evalGroupId"}, false, true, false);
      assertNotNull(l);
      assertEquals(0, l.size());

      // test that the get active part works
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE1_REF}, true, true, false);
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));

      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE2_REF}, true, true, false);
      assertNotNull(l);
      assertEquals(0, l.size());

      // test that the get active plus anon works
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.SITE1_REF}, true, true, true);
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationActive.getId() ));
      assertTrue(ids.contains( etdl.evaluationActiveUntaken.getId() ));

      // test getting from an invalid evalGroupId
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {EvalTestDataLoad.INVALID_CONTEXT}, true, true, false);
      assertNotNull(l);
      assertEquals(0, l.size());		

      // test getting all anonymous evals
      l = evaluationDao.getEvaluationsByEvalGroups(
            new String[] {}, false, false, true);
      assertNotNull(l);
      assertEquals(1, l.size());		
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.evaluationActiveUntaken.getId() ));

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#getAnswers(java.lang.Long, java.lang.Long)}.
    */
   public void testGetAnswers() {
      Set<EvalAnswer> s = null;
      List<EvalAnswer> l = null;
      List<Long> ids = null;

      s = etdl.response2.getAnswers();
      assertNotNull(s);
      assertEquals(2, s.size());
      ids = EvalTestDataLoad.makeIdList(s);
      assertTrue(ids.contains( etdl.answer2_2.getId() ));
      assertTrue(ids.contains( etdl.answer2_5.getId() ));

      l = evaluationDao.getAnswers(etdl.item2.getId(), etdl.evaluationClosed.getId(), null);
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer2_2.getId() ));
      assertTrue(ids.contains( etdl.answer3_2.getId() ));

      // test restricting to groups
      l = evaluationDao.getAnswers(etdl.item2.getId(), etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE1_REF});
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer2_2.getId() ));

      l = evaluationDao.getAnswers(etdl.item2.getId(), etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE2_REF});
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer3_2.getId() ));

      l = evaluationDao.getAnswers(etdl.item5.getId(), etdl.evaluationClosed.getId(), null);
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.answer2_5.getId() ));

      // test item that is not in this evaluation
      l = evaluationDao.getAnswers(etdl.item3.getId(), etdl.evaluationClosed.getId(), null);
      assertNotNull(l);
      assertEquals(0, l.size());

      // test invalid eval id
      // TODO - this should probably throw an exception
      l = evaluationDao.getAnswers(etdl.item1.getId(), Long.valueOf(999), null);
      assertNotNull(l);
      assertEquals(0, l.size());

      // test invalid item id
      // TODO - this should probably throw an exception
      l = evaluationDao.getAnswers(Long.valueOf(999), etdl.evaluationClosed.getId(), null);
      assertNotNull(l);
      assertEquals(0, l.size());
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#removeTemplateItems(org.sakaiproject.evaluation.model.EvalTemplateItem[])}.
    */
   public void testRemoveTemplateItems() {

      // test removing a single templateItem
      EvalTemplateItem eti1 = (EvalTemplateItem) evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem1User.getId());

      // verify that the item/template link exists before removal
      assertNotNull( eti1 );
      assertNotNull( eti1.getItem() );
      assertNotNull( eti1.getTemplate() );
      assertNotNull( eti1.getItem().getTemplateItems() );
      assertNotNull( eti1.getTemplate().getTemplateItems() );
      assertFalse( eti1.getItem().getTemplateItems().isEmpty() );
      assertFalse( eti1.getTemplate().getTemplateItems().isEmpty() );
      assertTrue( eti1.getItem().getTemplateItems().contains( eti1 ) );
      assertTrue( eti1.getTemplate().getTemplateItems().contains( eti1 ) );
      int itemsSize = eti1.getItem().getTemplateItems().size();
      int templatesSize = eti1.getTemplate().getTemplateItems().size();

      // test removing templateItem OK
      evaluationDao.removeTemplateItems( new EvalTemplateItem[] {etdl.templateItem1User} );
      assertNull( evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem1User.getId()) );

      // verify that the item/template link no longer exists
      assertNotNull( eti1.getItem().getTemplateItems() );
      assertNotNull( eti1.getTemplate().getTemplateItems() );
      assertFalse( eti1.getItem().getTemplateItems().isEmpty() );
      assertFalse( eti1.getTemplate().getTemplateItems().isEmpty() );
      assertEquals( itemsSize-1, eti1.getItem().getTemplateItems().size() );
      assertEquals( templatesSize-1, eti1.getTemplate().getTemplateItems().size() );
      assertTrue(! eti1.getItem().getTemplateItems().contains( eti1 ) );
      assertTrue(! eti1.getTemplate().getTemplateItems().contains( eti1 ) );

      // test removing a group of templateItems (item 3 and 5 from UnUsed)
      EvalTemplateItem eti3 = (EvalTemplateItem) evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem3U.getId());
      EvalTemplateItem eti5 = (EvalTemplateItem) evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem5U.getId());

      // verify that the item/template link exists before removal
      assertNotNull( eti3 );
      assertNotNull( eti3.getItem() );
      assertNotNull( eti3.getTemplate() );
      assertNotNull( eti3.getItem().getTemplateItems() );
      assertNotNull( eti3.getTemplate().getTemplateItems() );
      assertFalse( eti3.getItem().getTemplateItems().isEmpty() );
      assertFalse( eti3.getTemplate().getTemplateItems().isEmpty() );
      assertTrue( eti3.getItem().getTemplateItems().contains( eti3 ) );
      assertTrue( eti3.getTemplate().getTemplateItems().contains( eti3 ) );
      int itemsSize3 = eti3.getItem().getTemplateItems().size();

      assertNotNull( eti5 );
      assertNotNull( eti5.getItem() );
      assertNotNull( eti5.getTemplate() );
      assertNotNull( eti5.getItem().getTemplateItems() );
      assertNotNull( eti5.getTemplate().getTemplateItems() );
      assertFalse( eti5.getItem().getTemplateItems().isEmpty() );
      assertFalse( eti5.getTemplate().getTemplateItems().isEmpty() );
      assertTrue( eti5.getItem().getTemplateItems().contains( eti5 ) );
      assertTrue( eti5.getTemplate().getTemplateItems().contains( eti5 ) );
      int itemsSize5 = eti5.getItem().getTemplateItems().size();

      // test removing templateItem OK
      evaluationDao.removeTemplateItems( new EvalTemplateItem[] {etdl.templateItem3U, etdl.templateItem5U} );
      assertNull( evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem3U.getId()) );
      assertNull( evaluationDao.findById(EvalTemplateItem.class, etdl.templateItem5U.getId()) );

      // verify that the item/template link no longer exists
      assertNotNull( eti3.getItem().getTemplateItems() );
      assertFalse( eti3.getItem().getTemplateItems().isEmpty() );
      assertEquals( itemsSize3-1, eti3.getItem().getTemplateItems().size() );
      assertTrue(! eti3.getItem().getTemplateItems().contains( eti3 ) );

      assertNotNull( eti5.getItem().getTemplateItems() );
      assertFalse( eti5.getItem().getTemplateItems().isEmpty() );
      assertEquals( itemsSize5-1, eti5.getItem().getTemplateItems().size() );
      assertTrue(! eti5.getItem().getTemplateItems().contains( eti5 ) );

      // should be no items left in this template now
      assertNotNull( eti3.getTemplate().getTemplateItems() );
      assertTrue( eti3.getTemplate().getTemplateItems().isEmpty() );
      EvalTemplate template = (EvalTemplate) evaluationDao.findById(EvalTemplate.class, eti3.getTemplate().getId());
      assertNotNull( template );
      assertNotNull( template.getTemplateItems() );
      assertTrue( template.getTemplateItems().isEmpty() );

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#getTemplateItemsByTemplate(java.lang.Long, java.lang.String[], java.lang.String[], java.lang.String[])}.
    */
   public void testGetTemplateItemsByTemplate() {
      List<EvalTemplateItem> l = null;
      List<Long> ids = null;

      // test the basic return of items in the template
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdmin.getId(), 
            null, null, null);
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateItem2A.getId() ));
      assertTrue(ids.contains( etdl.templateItem3A.getId() ));
      assertTrue(ids.contains( etdl.templateItem5A.getId() ));

      // check that the return order is correct
      assertEquals( 1, ((EvalTemplateItem)l.get(0)).getDisplayOrder().intValue() );
      assertEquals( 2, ((EvalTemplateItem)l.get(1)).getDisplayOrder().intValue() );
      assertEquals( 3, ((EvalTemplateItem)l.get(2)).getDisplayOrder().intValue() );

      // test getting just the top level items
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdminComplex.getId(), 
            null, null, null);
      assertNotNull(l);
      assertEquals(0, l.size());

      // test getting instructor items
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdminComplex.getId(), 
            null, new String[] { EvalTestDataLoad.MAINT_USER_ID }, null);
      assertNotNull(l);
      assertEquals(1, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateItem10AC1.getId() ));

      // test getting course items
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdminComplex.getId(), 
            null, null, 
            new String[] { EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF });
      assertNotNull(l);
      assertEquals(2, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateItem10AC2.getId() ));
      assertTrue(ids.contains( etdl.templateItem10AC3.getId() ));

      // test getting both together
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdminComplex.getId(), 
            null, new String[] { EvalTestDataLoad.MAINT_USER_ID }, 
            new String[] { EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF });
      assertNotNull(l);
      assertEquals(3, l.size());
      ids = EvalTestDataLoad.makeIdList(l);
      assertTrue(ids.contains( etdl.templateItem10AC1.getId() ));
      assertTrue(ids.contains( etdl.templateItem10AC2.getId() ));
      assertTrue(ids.contains( etdl.templateItem10AC3.getId() ));

      // test that a bunch of invalid stuff simply returns no results
      l = evaluationDao.getTemplateItemsByTemplate(etdl.templateAdminComplex.getId(), 
            new String[] { EvalTestDataLoad.INVALID_CONSTANT_STRING }, 
            new String[] { EvalTestDataLoad.INVALID_CONSTANT_STRING, EvalTestDataLoad.INVALID_CONSTANT_STRING }, 
            new String[] { EvalTestDataLoad.INVALID_CONSTANT_STRING, EvalTestDataLoad.INVALID_CONSTANT_STRING, EvalTestDataLoad.INVALID_CONSTANT_STRING });
      assertNotNull(l);
      assertEquals(0, l.size());

   }

   public void testGetResponseIds() {
      List<Long> l = null;

      l = evaluationDao.getResponseIds(etdl.evaluationClosed.getId(), null, null, null);
      assertNotNull(l);
      assertEquals(3, l.size());
      assertTrue( l.contains(etdl.response2.getId()) );
      assertTrue( l.contains(etdl.response3.getId()) );
      assertTrue( l.contains(etdl.response6.getId()) );

      l = evaluationDao.getResponseIds(etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE1_REF, EvalTestDataLoad.SITE2_REF}, null, null);
      assertNotNull(l);
      assertEquals(3, l.size());
      assertTrue( l.contains(etdl.response2.getId()) );
      assertTrue( l.contains(etdl.response3.getId()) );
      assertTrue( l.contains(etdl.response6.getId()) );

      l = evaluationDao.getResponseIds(etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE1_REF}, null, null);
      assertNotNull(l);
      assertEquals(1, l.size());
      assertTrue( l.contains(etdl.response2.getId()) );

      // test invalid evalid
      l = evaluationDao.getResponseIds(EvalTestDataLoad.INVALID_LONG_ID, null, null, null);
      assertNotNull(l);
      assertEquals(0, l.size());

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#getEvalCategories(String)}
    */
   public void testGetEvalCategories() {
      List<String> l = null;

      // test the basic return of categories
      l = evaluationDao.getEvalCategories(null);
      assertNotNull(l);
      assertEquals(2, l.size());
      assertTrue( l.contains(EvalTestDataLoad.EVAL_CATEGORY_1) );
      assertTrue( l.contains(EvalTestDataLoad.EVAL_CATEGORY_2) );

      // test the return of cats for a user
      l = evaluationDao.getEvalCategories(EvalTestDataLoad.MAINT_USER_ID);
      assertNotNull(l);
      assertEquals(1, l.size());
      assertTrue( l.contains(EvalTestDataLoad.EVAL_CATEGORY_1) );

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#getNodeIdForEvalGroup(java.lang.String)}.
    */
   public void testGetNodeIdForEvalGroup() {
      String nodeId = null; 

      nodeId = evaluationDao.getNodeIdForEvalGroup(EvalTestDataLoad.SITE1_REF);
      assertNotNull(nodeId);
      assertEquals(EvalTestDataLoad.NODE_ID1, nodeId);

      nodeId = evaluationDao.getNodeIdForEvalGroup(EvalTestDataLoad.SITE2_REF);
      assertNotNull(nodeId);
      assertEquals(EvalTestDataLoad.NODE_ID1, nodeId);

      nodeId = evaluationDao.getNodeIdForEvalGroup(EvalTestDataLoad.SITE3_REF);
      assertNotNull(nodeId);
      assertEquals(EvalTestDataLoad.NODE_ID2, nodeId);

      nodeId = evaluationDao.getNodeIdForEvalGroup("xxxxxxxxxxxxxxxxx");
      assertNull(nodeId);
   }

   public void testGetTemplateItemsByEvaluation() {
      List<EvalTemplateItem> templateItems = null;
      
      templateItems = evaluationDao.getTemplateItemsByEvaluation(etdl.evaluationActive.getId(), null, null, null);
      assertNotNull(templateItems);
      assertEquals(2, templateItems.size());

      templateItems = evaluationDao.getTemplateItemsByEvaluation(etdl.evaluationClosed.getId(), null, null, null);
      assertNotNull(templateItems);
      assertEquals(3, templateItems.size());

      try {
         templateItems = evaluationDao.getTemplateItemsByEvaluation(EvalTestDataLoad.INVALID_LONG_ID, null, null, null);
         fail("Should have thrown an exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   public void testGetTemplateIdsForEvaluation() {
      List<Long> templateIds = null;

      templateIds = evaluationDao.getTemplateIdsForEvaluation(etdl.evaluationActive.getId());
      assertNotNull(templateIds);
      assertEquals(1, templateIds.size());
      assertTrue( templateIds.contains( etdl.templateUser.getId() ) );

      templateIds = evaluationDao.getTemplateIdsForEvaluation(etdl.evaluationClosed.getId());
      assertNotNull(templateIds);
      assertEquals(2, templateIds.size());
      assertTrue( templateIds.contains( etdl.templateAdmin.getId() ) );
      assertTrue( templateIds.contains( etdl.templateAdminComplex.getId() ) );

      templateIds = evaluationDao.getTemplateIdsForEvaluation(EvalTestDataLoad.INVALID_LONG_ID);
      assertNotNull(templateIds);
      assertEquals(0, templateIds.size());
   }

   public void testGetResponseUserIds() {
      Set<String> userIds = null;

      // check getting responders from complete evaluation
      userIds = evaluationDao.getResponseUserIds(etdl.evaluationClosed.getId(), null);
      assertNotNull(userIds);
      assertEquals(2, userIds.size());
      assertTrue(userIds.contains(EvalTestDataLoad.USER_ID));
      assertTrue(userIds.contains(EvalTestDataLoad.STUDENT_USER_ID));

      // test getting from subset of the groups
      userIds = evaluationDao.getResponseUserIds(etdl.evaluationClosed.getId(), new String[] {EvalTestDataLoad.SITE1_REF});
      assertNotNull(userIds);
      assertEquals(1, userIds.size());
      assertTrue(userIds.contains(EvalTestDataLoad.USER_ID));

      // test getting none
      userIds = evaluationDao.getResponseUserIds(etdl.evaluationActiveUntaken.getId(), null);
      assertNotNull(userIds);
      assertEquals(0, userIds.size());

      // test using invalid group ids retrieves no results
      userIds = evaluationDao.getResponseUserIds(etdl.evaluationClosed.getId(), new String[] {"xxxxxx", "fakeyandnotreal"});
      assertNotNull(userIds);
      assertEquals(0, userIds.size());

   }

   public void testGetViewableEvalGroupIds() {
      Set<String> evalGroupIds = null;

      // check for groups that are fully enabled
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationClosed.getId(), EvalConstants.PERM_BE_EVALUATED, null);
      assertNotNull(evalGroupIds);
      assertEquals(2, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign3.getEvalGroupId()));
      assertTrue(evalGroupIds.contains(etdl.assign4.getEvalGroupId()));

      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationClosed.getId(), EvalConstants.PERM_TAKE_EVALUATION, null);
      assertNotNull(evalGroupIds);
      assertEquals(2, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign3.getEvalGroupId()));
      assertTrue(evalGroupIds.contains(etdl.assign4.getEvalGroupId()));

      // check for mixture
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNewAdmin.getId(), EvalConstants.PERM_BE_EVALUATED, null);
      assertNotNull(evalGroupIds);
      assertEquals(2, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign7.getEvalGroupId()));
      assertTrue(evalGroupIds.contains(etdl.assignGroupProvided.getEvalGroupId()));

      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNewAdmin.getId(), EvalConstants.PERM_TAKE_EVALUATION, null);
      assertNotNull(evalGroupIds);
      assertEquals(1, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign6.getEvalGroupId()));

      // check for unassigned to return none
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNew.getId(), EvalConstants.PERM_BE_EVALUATED, null);
      assertNotNull(evalGroupIds);
      assertEquals(0, evalGroupIds.size());

      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNew.getId(), EvalConstants.PERM_TAKE_EVALUATION, null);
      assertNotNull(evalGroupIds);
      assertEquals(0, evalGroupIds.size());

      // check that other perms return nothing
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNewAdmin.getId(), EvalConstants.PERM_ASSIGN_EVALUATION, null);
      assertNotNull(evalGroupIds);
      assertEquals(0, evalGroupIds.size());

      // check for limits on the returns
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationClosed.getId(), EvalConstants.PERM_BE_EVALUATED, 
            new String[] {etdl.assign3.getEvalGroupId()});
      assertNotNull(evalGroupIds);
      assertEquals(1, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign3.getEvalGroupId()));

      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationNewAdmin.getId(), EvalConstants.PERM_BE_EVALUATED, 
            new String[] {etdl.assign7.getEvalGroupId()});
      assertNotNull(evalGroupIds);
      assertEquals(1, evalGroupIds.size());
      assertTrue(evalGroupIds.contains(etdl.assign7.getEvalGroupId()));

      // check for limits on the returns which limit it to none
      evalGroupIds = evaluationDao.getViewableEvalGroupIds(etdl.evaluationClosed.getId(), EvalConstants.PERM_BE_EVALUATED, 
            new String[] {EvalTestDataLoad.INVALID_CONSTANT_STRING});
      assertNotNull(evalGroupIds);
      assertEquals(0, evalGroupIds.size());

      // check for null evaluation id
      try {
         evaluationDao.getViewableEvalGroupIds(null, EvalConstants.PERM_ASSIGN_EVALUATION, null);
         fail("Should have thrown an exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }

   }
   
   
   
   
   
   
   // LOCKING tests

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#lockScale(org.sakaiproject.evaluation.model.EvalScale, java.lang.Boolean)}.
    */
   public void testLockScale() {

      // check that locked scale gets unlocked (no locking item)
      assertTrue( scaleLocked.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockScale( scaleLocked, Boolean.FALSE ) );
      assertFalse( scaleLocked.getLocked().booleanValue() );
      // check that unlocking an unlocked scale is not a problem
      assertFalse( evaluationDao.lockScale( scaleLocked, Boolean.FALSE ) );

      // check that locked scale that is locked by an item cannot be unlocked
      EvalScale scale1 = (EvalScale) evaluationDao.findById(EvalScale.class, etdl.scale1.getId());
      assertTrue( scale1.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockScale( scale1, Boolean.FALSE ) );
      assertTrue( scale1.getLocked().booleanValue() );
      // check that locking a locked scale is not a problem
      assertFalse( evaluationDao.lockScale( scale1, Boolean.TRUE ) );

      // check that new scale cannot be unlocked
      try {
         evaluationDao.lockScale( 
               new EvalScale(new Date(), 
                     EvalTestDataLoad.ADMIN_USER_ID, "new scale", 
                     EvalConstants.SCALE_MODE_SCALE, EvalConstants.SHARING_PRIVATE, Boolean.FALSE),
                     Boolean.FALSE
         );
         fail("Should have thrown an exception");
      } catch (IllegalStateException e) {
         assertNotNull(e);
      }

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#lockItem(org.sakaiproject.evaluation.model.EvalItem, java.lang.Boolean)}.
    */
   public void testLockItem() {

      // check that unlocked item gets locked (no scale)
      assertFalse( etdl.item7.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockItem( etdl.item7, Boolean.TRUE ) );
      assertTrue( etdl.item7.getLocked().booleanValue() );

      // check that locked item does nothing bad if locked again (no scale, not used)
      assertTrue( itemLocked.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockItem( itemLocked, Boolean.TRUE ) );
      assertTrue( itemLocked.getLocked().booleanValue() );

      // check that locked item gets unlocked (no scale, not used)
      assertTrue( itemLocked.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockItem( itemLocked, Boolean.FALSE ) );
      assertFalse( itemLocked.getLocked().booleanValue() );

      // check that locked item that is locked by a template cannot be unlocked
      assertTrue( etdl.item1.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockItem( etdl.item1, Boolean.FALSE ) );
      assertTrue( etdl.item1.getLocked().booleanValue() );

      // check that locked item that is locked by a template can be locked without exception
      assertTrue( etdl.item1.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockItem( etdl.item1, Boolean.TRUE ) );
      assertTrue( etdl.item1.getLocked().booleanValue() );

      // verify that associated scale is unlocked
      assertFalse( itemUnlocked.getScale().getLocked().booleanValue() );

      // check that unlocked item gets locked (scale)
      assertFalse( itemUnlocked.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockItem( itemUnlocked, Boolean.TRUE ) );
      assertTrue( itemUnlocked.getLocked().booleanValue() );

      // verify that associated scale gets locked
      assertTrue( itemUnlocked.getScale().getLocked().booleanValue() );

      // check that locked item gets unlocked (scale)
      assertTrue( itemUnlocked.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockItem( itemUnlocked, Boolean.FALSE ) );
      assertFalse( itemUnlocked.getLocked().booleanValue() );

      // verify that associated scale gets unlocked
      assertFalse( itemUnlocked.getScale().getLocked().booleanValue() );

      // check that locked item gets unlocked (scale locked by another item)
      assertTrue( etdl.item4.getScale().getLocked().booleanValue() );
      assertTrue( evaluationDao.lockItem( etdl.item4, Boolean.TRUE ) );
      assertTrue( etdl.item4.getLocked().booleanValue() );

      assertTrue( evaluationDao.lockItem( etdl.item4, Boolean.FALSE ) );
      assertFalse( etdl.item4.getLocked().booleanValue() );

      // verify that associated scale does not get unlocked
      assertTrue( etdl.item4.getScale().getLocked().booleanValue() );

      // check that new item cannot be locked/unlocked
      try {
         evaluationDao.lockItem(
               new EvalItem( new Date(), EvalTestDataLoad.ADMIN_USER_ID, 
                     "something", EvalConstants.SHARING_PRIVATE, 
                     EvalConstants.ITEM_TYPE_HEADER, Boolean.FALSE),
                     Boolean.TRUE);
         fail("Should have thrown an exception");
      } catch (IllegalStateException e) {
         assertNotNull(e);
      }

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#lockTemplate(org.sakaiproject.evaluation.model.EvalTemplate, java.lang.Boolean)}.
    */
   public void testLockTemplate() {

      // check that unlocked template gets locked (no items)
      assertFalse( etdl.templateAdminNoItems.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateAdminNoItems, Boolean.TRUE ) );
      assertTrue( etdl.templateAdminNoItems.getLocked().booleanValue() );

      // check that locked template is ok with getting locked again (no problems)
      assertTrue( etdl.templateAdminNoItems.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockTemplate( etdl.templateAdminNoItems, Boolean.TRUE ) );
      assertTrue( etdl.templateAdminNoItems.getLocked().booleanValue() );

      // check that locked template gets unlocked (no items)
      assertTrue( etdl.templateAdminNoItems.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateAdminNoItems, Boolean.FALSE ) );
      assertFalse( etdl.templateAdminNoItems.getLocked().booleanValue() );

      // check that locked template that is locked by an evaluation cannot be unlocked
      assertTrue( etdl.templateUser.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockTemplate( etdl.templateUser, Boolean.FALSE ) );
      assertTrue( etdl.templateUser.getLocked().booleanValue() );

      // check that locked template that is locked by an evaluation can be locked without exception
      assertTrue( etdl.templateUser.getLocked().booleanValue() );
      assertFalse( evaluationDao.lockTemplate( etdl.templateUser, Boolean.TRUE ) );
      assertTrue( etdl.templateUser.getLocked().booleanValue() );

      // check that unlocked template gets locked (items)
      assertFalse( etdl.item6.getLocked().booleanValue() );
      assertFalse( etdl.templateUserUnused.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateUserUnused, Boolean.TRUE ) );
      assertTrue( etdl.templateUserUnused.getLocked().booleanValue() );

      // verify that related items are locked also
      assertTrue( etdl.item6.getLocked().booleanValue() );

      // check that locked template gets unlocked (items)
      assertTrue( etdl.templateUserUnused.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateUserUnused, Boolean.FALSE ) );
      assertFalse( etdl.templateUserUnused.getLocked().booleanValue() );

      // verify that related items are unlocked also
      assertFalse( etdl.item6.getLocked().booleanValue() );

      // check unlocked template with locked items can be locked
      assertFalse( etdl.templateUnused.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateUnused, Boolean.TRUE ) );
      assertTrue( etdl.templateUnused.getLocked().booleanValue() );

      // check that locked template gets unlocked (items locked by another template)
      assertTrue( etdl.item3.getLocked().booleanValue() );
      assertTrue( etdl.item5.getLocked().booleanValue() );
      assertTrue( etdl.templateUnused.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockTemplate( etdl.templateUnused, Boolean.FALSE ) );
      assertFalse( etdl.templateUnused.getLocked().booleanValue() );

      // verify that associated items locked by other template do not get unlocked
      assertTrue( etdl.item3.getLocked().booleanValue() );
      assertTrue( etdl.item5.getLocked().booleanValue() );

      // check that new template cannot be locked/unlocked
      try {
         evaluationDao.lockTemplate(
               new EvalTemplate(new Date(), EvalTestDataLoad.ADMIN_USER_ID, 
                     EvalConstants.TEMPLATE_TYPE_STANDARD, "new template one", 
                     "description", EvalConstants.SHARING_PRIVATE, EvalTestDataLoad.NOT_EXPERT, 
                     "expert desc", null, EvalTestDataLoad.LOCKED),
                     Boolean.TRUE);
         fail("Should have thrown an exception");
      } catch (IllegalStateException e) {
         assertNotNull(e);
      }

   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#lockEvaluation(org.sakaiproject.evaluation.model.EvalEvaluation)}.
    */
   public void testLockEvaluation() {

      // check that unlocked evaluation gets locked
      assertFalse( etdl.templatePublicUnused.getLocked().booleanValue() );
      assertFalse( evalUnLocked.getLocked().booleanValue() );
      assertTrue( evaluationDao.lockEvaluation( evalUnLocked ) );
      assertTrue( evalUnLocked.getLocked().booleanValue() );

      // verify that associated template gets locked
      assertTrue( etdl.templatePublicUnused.getLocked().booleanValue() );

      // check that new evaluation cannot be locked
      try {
         evaluationDao.lockEvaluation(
               new EvalEvaluation(new Date(), EvalTestDataLoad.MAINT_USER_ID, "Eval new", null, 
                     etdl.tomorrow, etdl.threeDaysFuture, etdl.threeDaysFuture, etdl.fourDaysFuture, null, null,
                     EvalConstants.EVALUATION_STATE_INQUEUE, EvalConstants.INSTRUCTOR_OPT_IN, 
                     new Integer(1), null, null, null, null, etdl.templatePublic, null, null,
                     Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, EvalTestDataLoad.UNLOCKED,
                     EvalConstants.EVALUATION_AUTHCONTROL_AUTH_REQ, null)
         );
         fail("Should have thrown an exception");
      } catch (IllegalStateException e) {
         assertNotNull(e);
      }

   }


   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#isUsedScale(java.lang.Long)}.
    */
   public void testIsUsedScale() {
      assertTrue( evaluationDao.isUsedScale( etdl.scale1.getId() ) );
      assertTrue( evaluationDao.isUsedScale( etdl.scale2.getId() ) );
      assertFalse( evaluationDao.isUsedScale( etdl.scale3.getId() ) );
      assertFalse( evaluationDao.isUsedScale( etdl.scale4.getId() ) );
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#isUsedItem(java.lang.Long)}.
    */
   public void testIsUsedItem() {
      assertTrue( evaluationDao.isUsedItem( etdl.item1.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item2.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item3.getId() ) );
      assertFalse( evaluationDao.isUsedItem( etdl.item4.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item5.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item6.getId() ) );
      assertFalse( evaluationDao.isUsedItem( etdl.item7.getId() ) );
      assertFalse( evaluationDao.isUsedItem( etdl.item8.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item9.getId() ) );
      assertTrue( evaluationDao.isUsedItem( etdl.item10.getId() ) );
   }

   /**
    * Test method for {@link org.sakaiproject.evaluation.dao.EvaluationDaoImpl#isUsedTemplate(java.lang.Long)}.
    */
   public void testIsUsedTemplate() {
      assertTrue( evaluationDao.isUsedTemplate( etdl.templateAdmin.getId() ) );
      assertFalse( evaluationDao.isUsedTemplate( etdl.templateAdminBlock.getId() ) );
      assertFalse( evaluationDao.isUsedTemplate( etdl.templateAdminComplex.getId() ) );
      assertFalse( evaluationDao.isUsedTemplate( etdl.templateAdminNoItems.getId() ) );
      assertTrue( evaluationDao.isUsedTemplate( etdl.templatePublic.getId() ) );
      assertTrue( evaluationDao.isUsedTemplate( etdl.templatePublicUnused.getId() ) ); // used in this file
      assertFalse( evaluationDao.isUsedTemplate( etdl.templateUnused.getId() ) );
      assertTrue( evaluationDao.isUsedTemplate( etdl.templateUser.getId() ) );
      assertFalse( evaluationDao.isUsedTemplate( etdl.templateUserUnused.getId() ) );
   }



   /**
    * Something about this test means it has to run at the end or the others fail
    */
   public void testLockAndExecuteRunnable() {
      final int[] checker = new int[] { 0 }; 
      Runnable toExecute = new Runnable() {
         public void run() {
            checker[0] = checker[0] + 1;
         }
      };

      // make sure no locks exist
      assertEquals(0, evaluationDao.countAll(EvalLock.class));

      assertEquals(0, checker[0]);
      assertTrue( evaluationDao.lockAndExecuteRunnable("AZ.lock", "AZ1", toExecute) );
      assertEquals(1, checker[0]);

      // make sure locks were cleared
      assertEquals(0, evaluationDao.countAll(EvalLock.class));

      // insert a fake lock and test that it blocks the execute
      EvalLock fakeLock = new EvalLock("FAKE.lock", "AZ2");
      evaluationDao.save(fakeLock);

      assertEquals(1, evaluationDao.countAll(EvalLock.class));

      assertEquals(1, checker[0]);
      assertFalse( evaluationDao.lockAndExecuteRunnable("FAKE.lock", "AZ1", toExecute) );
      assertEquals(1, checker[0]);

      evaluationDao.delete(fakeLock);

      // make sure locks were cleared
      assertEquals(0, evaluationDao.countAll(EvalLock.class));


      // test the a failing execute still cleans up the locks
      Runnable toExecuteFails = new Runnable() {
         public void run() {
            checker[0] = checker[0] + 1;
            throw new IllegalStateException("Oh Noes!");
         }
      };

      assertEquals(1, checker[0]);
      assertNull( evaluationDao.lockAndExecuteRunnable("AZ.lock", "AZ1", toExecuteFails) );
      assertEquals(2, checker[0]);
      assertEquals(0, evaluationDao.countAll(EvalLock.class));

      // check invalid arguments cause failure
      try {
         evaluationDao.lockAndExecuteRunnable("AZ.lock", null, toExecute);
         fail("Should have thrown an exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
      try {
         evaluationDao.lockAndExecuteRunnable(null, "AZ1", toExecute);
         fail("Should have thrown an exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
      try {
         evaluationDao.lockAndExecuteRunnable("AZ.lock", "AZ1", null);
         fail("Should have thrown an exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e);
      }
   }

   /**
    * Add anything that supports the unit tests below here
    */

}
