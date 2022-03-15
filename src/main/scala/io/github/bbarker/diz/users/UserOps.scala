package io.github.bbarker.diz.users

import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import discord4j.core.`object`.entity.{Message, User}

object UserOps:

  extension (user: User)
    def mention: String = s"<@${user.getUserData.id}>"
    def mentionPre: String = s"${user.mention}, "
    def mentionPost: String = s", ${user.mention}"
  //
  extension (userOpt: Option[User])
    def mention: String = userOpt.fold("")(_.mention)
    def mentionPre: String = userOpt.fold("")(_.mentionPre)
    def mentionPost: String = userOpt.fold("")(_.mentionPost)
  //
  extension (userOpt: Optional[User])
    def mention: String = userOpt.toScala.mention
    def mentionPre: String = userOpt.toScala.mentionPre
    def mentionPost: String = userOpt.toScala.mentionPost
  //
  extension (userMessage: Message)
    def mentionAuthor: String = userMessage.getAuthor.mention
    def mentionAuthorPre: String = userMessage.getAuthor.mentionPre
    def mentionAuthorPost: String = userMessage.getAuthor.mentionPost
    //
    def mentionMentions: String =
      userMessage.getUserMentions.asScala.map(_.mention).mkString(", ")
    def mentionMentionsPre: String =
      val users = userMessage.getUserMentions.asScala
      val sep = users.length match
        case 0 => ""
        case 1 => ", "
        case _ => ": "
      users.map(_.mention).mkString(", ") ++ sep
    def mentionMentionsPost: String =
      val users = userMessage.getUserMentions.asScala
      val sep = users.length match
        case 0 => ""
        case 1 => ", "
        case _ => ": "
      sep ++ users.map(_.mention).mkString(", ")
