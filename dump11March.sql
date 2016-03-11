-- MySQL dump 10.13  Distrib 5.5.27, for Win64 (x86)
--
-- Host: 104.168.163.65    Database: mazehunt
-- ------------------------------------------------------
-- Server version	5.0.95

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Not dumping tablespaces as no INFORMATION_SCHEMA.FILES table on this server
--

--
-- Table structure for table `explored_nodes`
--

DROP TABLE IF EXISTS `explored_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `explored_nodes` (
  `email_id` varchar(200) NOT NULL,
  `question_id` int(11) default NULL,
  `explored_timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `explored_nodes`
--

LOCK TABLES `explored_nodes` WRITE;
/*!40000 ALTER TABLE `explored_nodes` DISABLE KEYS */;
INSERT INTO `explored_nodes` VALUES ('gaurav.vashistha@impetus.co.in',101,'2016-03-10 08:27:51'),('ankita.a.singh@impetus.co.in',101,'2016-03-10 08:28:27'),('abhay.goel@impetus.co.in',101,'2016-03-10 08:34:50'),('sourav.gulati@impetus.co.in',101,'2016-03-10 08:57:19'),('isha.singh@impetus.co.in',101,'2016-03-10 09:01:25'),('amitb.kumar@impetus.co.in',101,'2016-03-10 09:17:30'),('gyanendra.m.dwivedi@impetus.co.in',101,'2016-03-10 09:20:01'),('anamika.pandey@impetus.co.in ',101,'2016-03-10 09:21:24'),('piyush.gupta@impetus.co.in',101,'2016-03-10 09:25:19'),('isha.singh@impetus.co.in',102,'2016-03-10 09:26:57'),('nakul.dev@impetus.co.in',101,'2016-03-10 09:29:12'),('chitrali.pingle@impetus.co.in',101,'2016-03-10 09:29:39'),('rahul.dev@impetus.co.in',101,'2016-03-10 09:31:35'),('vikas.singh@impetus.co.in',101,'2016-03-10 09:32:33'),('sourav.gulati@impetus.co.in',102,'2016-03-10 09:33:39'),('chandra.yadav@impetus.co.in',101,'2016-03-10 09:35:48'),('sumit.varshney@impetus.co.in',101,'2016-03-10 09:36:12'),('priyanka.babbar@impetus.co.in',101,'2016-03-10 09:36:14'),('null',101,'2016-03-10 09:36:22'),('anamika.pandey@impetus.co.in ',102,'2016-03-10 09:38:49'),('piyush.gupta@impetus.co.in',102,'2016-03-10 09:39:27'),('subodh.ray@impetus.co.in',101,'2016-03-10 09:40:30'),('anamika.pandey@impetus.co.in ',103,'2016-03-10 09:41:14'),('pawank.jashnani@impetus.co.in',101,'2016-03-10 09:42:10'),('amitb.kumar@impetus.co.in',102,'2016-03-10 09:43:14'),('aditya.srivastava@impetus.co.in',101,'2016-03-10 09:43:32'),('piyush.gupta@impetus.co.in',103,'2016-03-10 09:47:48'),('nakul.dev@impetus.co.in',102,'2016-03-10 09:53:43'),('gaurav.vashistha@impetus.co.in',102,'2016-03-10 09:55:09'),('gaurav.vashistha@impetus.co.in',102,'2016-03-10 09:55:12'),('gaurav.vashistha@impetus.co.in',103,'2016-03-10 09:57:39'),('gaurav.vashistha@impetus.co.in',103,'2016-03-10 09:57:39'),('aditya.srivastava@impetus.co.in',102,'2016-03-10 10:01:22'),('sourav.gulati@impetus.co.in',103,'2016-03-10 10:07:37'),('sumitag.gupta@impetus.co.in',101,'2016-03-10 10:17:08'),('thalpreet.singh@impetus.co.in',101,'2016-03-10 10:18:46'),('sumitag.gupta@impetus.co.in',102,'2016-03-10 10:24:25'),('gyanendra.m.dwivedi@impetus.co.in',102,'2016-03-10 10:24:49'),('rahul.dev@impetus.co.in',102,'2016-03-10 10:25:54'),('shikha.bhatia@impetus.co.in',101,'2016-03-10 10:27:41'),('rahul.dahiya@impetus.co.in',101,'2016-03-10 10:29:54'),('pawank.jashnani@impetus.co.in',102,'2016-03-10 10:32:02'),('rahul.dahiya@impetus.co.in',102,'2016-03-10 10:32:51'),('rahul.dahiya@impetus.co.in',103,'2016-03-10 10:33:30'),('rahul.dev@impetus.co.in',103,'2016-03-10 10:38:26'),('gyanendra.m.dwivedi@impetus.co.in',103,'2016-03-10 10:40:38'),('aditya.srivastava@impetus.co.in',103,'2016-03-10 10:42:34'),('nakul.dev@impetus.co.in',103,'2016-03-10 10:42:46'),('himanshu.pahuja@impetus.co.in',101,'2016-03-10 10:56:57'),('rahul.dahiya@impetus.co.in',114,'2016-03-10 10:59:04'),('amitb.kumar@impetus.co.in',103,'2016-03-10 11:00:54'),('sourav.gulati@impetus.co.in',114,'2016-03-10 11:01:27'),('anamika.pandey@impetus.co.in ',114,'2016-03-10 11:25:26'),('deepak.garg@impetus.co.in',101,'2016-03-10 11:34:56'),('null',103,'2016-03-10 11:49:40'),('rohit.c.kumar@impetus.co.in',101,'2016-03-10 11:57:00'),('ajayk.mishra@impetus.co.in',101,'2016-03-10 12:27:43'),('kushagra.mittal@impetus.co.in',101,'2016-03-10 12:46:45'),('null',114,'2016-03-10 12:47:35'),('raunaq.sharma@impetus.co.in',101,'2016-03-10 12:48:17'),('ajay.kakkar@impetus.co.in',101,'2016-03-10 12:48:29'),('null',102,'2016-03-10 12:49:05'),('amitb.kumar@impetus.co.in',114,'2016-03-10 12:53:50'),('amitb.kumar@impetus.co.in',116,'2016-03-10 12:56:30'),('anamika.pandey@impetus.co.in ',116,'2016-03-10 12:56:56'),('gyanendra.m.dwivedi@impetus.co.in',114,'2016-03-10 12:57:15'),('ankita.a.singh@impetus.co.in',102,'2016-03-10 13:02:09'),('sourav.gulati@impetus.co.in',116,'2016-03-10 13:04:09'),('chitrali.pingle@impetus.co.in',102,'2016-03-10 13:06:10'),('rahul.dahiya@impetus.co.in',116,'2016-03-10 13:19:07'),('rohit.c.kumar@impetus.co.in',102,'2016-03-10 13:25:38'),('Gaurav.vashistha@impetus.co.in',114,'2016-03-10 14:23:34'),('Gaurav.vashistha@impetus.co.in',114,'2016-03-10 14:23:35'),('kislay.kumar@impetus.co.in',101,'2016-03-10 18:33:31'),('kislay.kumar@impetus.co.in',102,'2016-03-10 18:39:58'),('aarti.grover@impetus.co.in',101,'2016-03-11 05:19:10'),('ujjawal.nayak@impetus.co.in',101,'2016-03-11 06:22:23'),('amit.pathak@impetus.co.in',101,'2016-03-11 06:38:45'),('amit.pathak@impetus.co.in',102,'2016-03-11 06:39:43'),('amit.pathak@impetus.co.in',103,'2016-03-11 06:40:10'),('rahul.dahiya@impetus.co.in',112,'2016-03-11 06:40:26'),('amit.pathak@impetus.co.in',114,'2016-03-11 06:40:48'),('amit.pathak@impetus.co.in',116,'2016-03-11 06:42:01'),('amit.pathak@impetus.co.in',112,'2016-03-11 06:42:38'),('amit.pathak@impetus.co.in',113,'2016-03-11 06:42:56'),('gyanendra.m.dwivedi@impetus.co.in',112,'2016-03-11 06:43:20'),('amit.pathak@impetus.co.in',104,'2016-03-11 06:43:28'),('gaurav.vashistha@impetus.co.in',116,'2016-03-11 06:43:53'),('amit.pathak@impetus.co.in',105,'2016-03-11 06:44:41'),('amit.pathak@impetus.co.in',106,'2016-03-11 06:45:26'),('gaurav.vashistha@impetus.co.in',112,'2016-03-11 06:46:18'),('gyanendra.m.dwivedi@impetus.co.in',104,'2016-03-11 06:52:24'),('gyanendra.m.dwivedi@impetus.co.in',105,'2016-03-11 06:57:38'),('gyanendra.m.dwivedi@impetus.co.in',116,'2016-03-11 07:03:00'),('gaurav.vashistha@impetus.co.in',113,'2016-03-11 07:05:01'),('gyanendra.m.dwivedi@impetus.co.in',106,'2016-03-11 07:11:06'),('sourav.gulati@impetus.co.in',112,'2016-03-11 07:14:28'),('gaurav.vashistha@impetus.co.in',104,'2016-03-11 07:23:39'),('rahul.dev@impetus.co.in',114,'2016-03-11 07:29:49'),('gyanendra.m.dwivedi@impetus.co.in',113,'2016-03-11 07:31:30'),('amitb.kumar@impetus.co.in',113,'2016-03-11 07:50:14'),('amitb.kumar@impetus.co.in',112,'2016-03-11 07:52:35'),('ramit.agarwal@impetus.co.in ',101,'2016-03-11 08:06:44'),('amitb.kumar@impetus.co.in',104,'2016-03-11 08:14:43'),('ujjawal.nayak@impetus.co.in',102,'2016-03-11 08:17:48'),('anamika.pandey@impetus.co.in',112,'2016-03-11 08:36:11'),('anamika.pandey@impetus.co.in',112,'2016-03-11 08:36:34'),('anamika.pandey@impetus.co.in',113,'2016-03-11 08:49:19'),('abhishek.j.bhardwaj@impetus.co.in',101,'2016-03-11 08:54:43'),('ramk.sharma@impetus.co.in',101,'2016-03-11 09:05:05'),('anamika.pandey@impetus.co.in ',104,'2016-03-11 09:11:42'),('anamika.pandey@impetus.co.in ',105,'2016-03-11 09:12:05'),('amitb.kumar@impetus.co.in',105,'2016-03-11 09:12:41'),('sumit.varshney@impetus.co.in',102,'2016-03-11 09:12:59'),('ramk.sharma@impetus.co.in',102,'2016-03-11 09:13:11'),('sumit.varshney@impetus.co.in',103,'2016-03-11 09:14:58'),('amitb.kumar@impetus.co.in',106,'2016-03-11 09:15:59'),('aarti.grover@impetus.co.in',102,'2016-03-11 09:16:53'),('anamika.pandey@impetus.co.in ',106,'2016-03-11 09:17:24'),('abhishek.j.bhardwaj@impetus.co.in',102,'2016-03-11 09:22:42'),('rohit.c.kumar@impetus.co.in',114,'2016-03-11 09:23:25'),('ankita.a.singh@impetus.co.in',114,'2016-03-11 09:35:25'),('thalpreet.singh@impetus.co.in',102,'2016-03-11 09:38:09'),('ankita.a.singh@impetus.co.in',116,'2016-03-11 09:46:06'),('ankita.a.singh@impetus.co.in',112,'2016-03-11 09:50:19'),('ankita.a.singh@impetus.co.in',103,'2016-03-11 09:53:02'),('rohit.c.kumar@impetus.co.in',103,'2016-03-11 09:57:55'),('rohit.c.kumar@impetus.co.in',116,'2016-03-11 09:58:41'),('null',104,'2016-03-11 10:36:38'),('rohit.c.kumar@impetus.co.in',112,'2016-03-11 10:41:55'),('sourav.gulati@impetus.co.in',113,'2016-03-11 10:44:31'),('sourav.gulati@impetus.co.in',104,'2016-03-11 10:44:50'),('rohit.c.kumar@impetus.co.in',113,'2016-03-11 10:55:52'),('rohit.c.kumar@impetus.co.in',104,'2016-03-11 11:03:01'),('sourav.gulati@impetus.co.in',106,'2016-03-11 11:04:43'),('sourav.gulati@impetus.co.in',105,'2016-03-11 11:28:19'),('ankita.a.singh@impetus.co.in',104,'2016-03-11 11:53:12'),('ankita.a.singh@impetus.co.in',113,'2016-03-11 11:53:53');
/*!40000 ALTER TABLE `explored_nodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_attribute`
--

DROP TABLE IF EXISTS `question_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `question_attribute` (
  `question_id` int(11) NOT NULL default '0',
  `ques_attribute` varchar(100) default NULL,
  `child_info` varchar(100) default NULL,
  PRIMARY KEY  (`question_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_attribute`
--

LOCK TABLES `question_attribute` WRITE;
/*!40000 ALTER TABLE `question_attribute` DISABLE KEYS */;
INSERT INTO `question_attribute` VALUES (101,'101:{\'color\':\'red\',\'shape\':\'dot\',\'label\':\'question101\'},','101:{ 102:{} },'),(102,'102:{\'color\':\'green\',\'shape\':\'dot\',\'label\':\'question102\'},','102:{ 103:{},114:{} },'),(103,'103:{\'color\':\'blue\',\'shape\':\'dot\',\'label\':\'question103\'},','103:{ 104:{} },'),(104,'104:{\'color\':\'orange\',\'shape\':\'dot\',\'label\':\'question104\'},','104:{ 105:{} },'),(105,'105:{\'color\':\'green\',\'shape\':\'dot\',\'label\':\'question105\'},','105:{ 106:{} },'),(106,'106:{\'color\':\'blue\',\'shape\':\'dot\',\'label\':\'question106\'},','106:{ 107:{},108:{} },'),(112,'112:{\'color\':\'blue\',\'shape\':\'dot\',\'label\':\'question112\'},','112:{  },'),(113,'113:{\'color\':\'orange\',\'shape\':\'dot\',\'label\':\'question113\'},','113:{  },'),(114,'114:{\'color\':\'green\',\'shape\':\'dot\',\'label\':\'question114\'},','114:{ 116:{} },'),(116,'116:{\'color\':\'orange\',\'shape\':\'dot\',\'label\':\'question116\'},','116:{ 112:{},113:{} },');
/*!40000 ALTER TABLE `question_attribute` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_info`
--

DROP TABLE IF EXISTS `question_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `question_info` (
  `id` int(11) NOT NULL,
  `question` varchar(1000) default NULL,
  `answer` varchar(200) default NULL,
  `childs` varchar(45) default NULL,
  `parent` varchar(45) default NULL,
  `score` int(3) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_info`
--

LOCK TABLES `question_info` WRITE;
/*!40000 ALTER TABLE `question_info` DISABLE KEYS */;
INSERT INTO `question_info` VALUES (101,'<img src=\"question/images/ElasticSearch-Geo-Replication.png\">\n                        Document..\n                        <br>Hint: Sometime the answer lies just in front of you invisible. \n                        <div style=\"color: #ffffff\">Richards</div>','Richards','101,102,103',NULL,10),(102,' <img src=question/images/findWords.png> <br>Arrange the words in dictionary order and take the first alphabet from each word for eg LION,CAT,CHEETAH,DOG then answer would be  CCDL ','BBCPRT','102,104,105',NULL,20),(103,'<img src=question/images/puzzle.png> <br>If you understand technology , you would see the picture and think of   ','namespace','103,106',NULL,20),(104,'<img src=question/images/D.png> <br>And Thats...., No Spaces and special characters Allowed?','ford',NULL,NULL,NULL),(105,'<img src=question/images/dim.png> <br>Wish you had one .., No Spaces Allowed?','diamondring',NULL,NULL,NULL),(106,'<img src=question/images/speed.png> <br>Wooh!!! .., No Spaces Allowed?','fastfood',NULL,NULL,NULL),(112,'<img src=question/images/number.gif> <br>Arrange hidden words in ascending order, No Spaces and special characters Allowed?','onetwothree',NULL,NULL,NULL),(113,'<img src=question/images/series.png> <br>Solve the image, No Spaces Allowed?','nine',NULL,NULL,NULL),(114,'<img src=question/images/monkeyfixingcomputer.jpg> <br> If text is impetus cipher text would be qlhrbza. what would be ciher text of oracle ?','kgwtjp',NULL,NULL,NULL),(116,'<img src=question/images/iq.jpeg> <br>classical approach to measure machine s IQ , No Spaces Allowed in Answer?','turingtest',NULL,NULL,NULL);
/*!40000 ALTER TABLE `question_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_travel`
--

DROP TABLE IF EXISTS `question_travel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `question_travel` (
  `question_id` int(11) default NULL,
  `traverse_id` int(11) default NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_travel`
--

LOCK TABLES `question_travel` WRITE;
/*!40000 ALTER TABLE `question_travel` DISABLE KEYS */;
INSERT INTO `question_travel` VALUES (105,106),(105,105),(104,105),(104,104),(103,104),(102,114),(103,103),(102,103),(102,102),(101,102),(101,101),(106,106),(106,107),(106,108),(107,107),(107,109),(109,109),(109,110),(110,110),(110,111),(108,108),(108,115),(115,115),(111,111),(114,114),(114,116),(116,116),(116,112),(116,113),(113,113),(112,112);
/*!40000 ALTER TABLE `question_travel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_table`
--

DROP TABLE IF EXISTS `user_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_table` (
  `email_id` varchar(100) NOT NULL,
  `password` varchar(45) default NULL,
  PRIMARY KEY  (`email_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_table`
--

LOCK TABLES `user_table` WRITE;
/*!40000 ALTER TABLE `user_table` DISABLE KEYS */;
INSERT INTO `user_table` VALUES ('aarti.grover@impetus.co.in','231757'),('abhay.goel@impetus.co.in','956198'),('abhishek.j.bhardwaj@impetus.co.in','798586'),('aditya.srivastava@impetus.co.in','106860'),('ajay.kakkar@impetus.co.in','525690'),('ajayk.mishra@impetus.co.in','028182'),('amit.pathak@impetus.co.in','118704'),('amitb.kumar@impetus.co.in','824483'),('anamika.pandey@impetus.co.in','029357'),('ankit.goyal@impetus.co.in','608554'),('ankita.a.singh@impetus.co.in','574480'),('anshu.maheshwari@impetus.co.in','937580'),('birendra.acharya@impetus.co.in','945434'),('chandra.yadav@impetus.co.in','681449'),('chitrali.pingle@impetus.co.in','633177'),('deepak.garg@impetus.co.in','676441'),('deepak.sinha@impetus.co.in','116196'),('devendra.madaan@impetus.co.in','950363'),('gaurav.vashistha@impetus.co.in','519672'),('geetanjali.gandhi@impetus.co.in','324947'),('gyanendra.m.dwivedi@impetus.co.in','047337'),('himanshu.pahuja@impetus.co.in','463253'),('isha.singh@impetus.co.in','729903'),('ishan.upamanyu@impetus.co.in','190592'),('kislay.kumar@impetus.co.in','114203'),('kushagra.mittal@impetus.co.in','735088'),('mahesh.tripathi@impetus.co.in','091674'),('manoj.pandey@impetus.co.in','877980'),('nakul.dev@impetus.co.in','689092'),('neeraj.kumar@impetus.co.in','830693'),('paarth.gurani@impetus.co.in','148246'),('pawank.jashnani@impetus.co.in','797905'),('piyush.gupta@impetus.co.in','637812'),('prabhat.sharma@impetus.co.in','029178'),('priyanka.babbar@impetus.co.in','673276'),('rahul.dahiya@impetus.co.in','763265'),('rahul.dev@impetus.co.in','630273'),('rahuldahiya@impetus.co.in','450408'),('ramit.agarwal@impetus.co.in','357603'),('ramk.sharma@impetus.co.in','452367'),('raunaq.sharma@impetus.co.in','302236'),('ravishankar.gupta@impetus.co.in','544479'),('rocky.bhatia@impetus.co.in','507752'),('rohit.c.kumar@impetus.co.in','365531'),('sanjay.jain@impetus.co.in','163847'),('sarthak.garg@impetus.co.in','895572'),('saurabh.gupta@impetus.co.in','524952'),('shashank.tiwari@impetus.co.in','680832'),('shikha.bhatia@impetus.co.in','598345'),('sourav.gulati@impetus.co.in','088157'),('subodh.ray@impetus.co.in','280461'),('sumit.kumar@impetus.co.in','549527'),('sumit.varshney@impetus.co.in','782708'),('sumitag.gupta@impetus.co.in','494834'),('Sunil.yadav@impetus.co.in','432301'),('suresh.sindhwani@impetus.co.in','579432'),('thalpreet.singh@impetus.co.in','704799'),('ujjawal.nayak@impetus.co.in','249106'),('vikas.singh@impetus.co.in','349983'),('vishal.patial@impetus.co.in','147600');
/*!40000 ALTER TABLE `user_table` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-03-11 18:41:42
