package org.example

import com.sun.mail.util.BASE64DecoderStream
import java.io.File
import java.io.IOException
import java.util.*
import javax.mail.*
import javax.mail.internet.MimeBodyPart


class EmailAttachmentReceiver {
    private var saveDirectory: String? = null

    fun downloadEmailAttachments(host: String?, port: String, userName: String?, password: String?) {
        val properties = Properties()

        // server setting
        properties["mail.imap.host"] = host
        properties["mail.imap.port"] = port

        // SSL setting
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        properties.setProperty("mail.imap.socketFactory.fallback", "false")
        properties.setProperty("mail.imap.socketFactory.port", port)
        properties.setProperty("mail.mime.base64.ignoreerrors", "true")
        properties.setProperty("mail.imap.partialfetch", "false")
        val session: Session = Session.getDefaultInstance(properties)
        try {
            // connects to the message store
            val store: Store = session.getStore("imap")
            store.connect(userName, password)

            // opens the inbox folder
            val folderInbox: Folder = store.getFolder("INBOX")
            folderInbox.open(Folder.READ_ONLY)

            // fetches new messages from server
            val arrayMessages: Array<Message> = folderInbox.messages
            for (i in arrayMessages.indices) {
                val message: Message = arrayMessages[i]
                val fromAddress: Array<Address> = message.from
                val from: String = fromAddress[0].toString()
                val subject: String = message.subject
                val sentDate: String = message.sentDate.toString()
                val contentType: String = message.contentType
                var messageContent = ""

                // store attachment file name, separated by comma
                var attachFiles = ""
                if (contentType.contains("multipart")) {
                    // content may contain attachments
                    val multiPart: Multipart = message.content as Multipart
                    val numberOfParts: Int = multiPart.count
                    for (partCount in 0 until numberOfParts) {
                        val part: MimeBodyPart = multiPart.getBodyPart(partCount) as MimeBodyPart
                        val attachment = Part.ATTACHMENT
                        if (attachment.equals(part.disposition, ignoreCase = true)) {
                            // this part is attachment
                            val fileName: String = part.fileName
                            if(fileName.contains(".ics")){
                                attachFiles += "$fileName, "
                                val content = part.content as BASE64DecoderStream
                                val contentBytes = content.readAllBytes()
                                val icsFileText = String(contentBytes, charset("UTF-8"))
                                parseDate(icsFileText)
                            }
                        } else {
                            // this part may be the message content
                            messageContent = part.content.toString()
                        }
                    }
                    if (attachFiles.length > 1) {
                        attachFiles = attachFiles.substring(0, attachFiles.length - 2)
                    }
                } else if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    val content: Any = message.content
                    messageContent = content.toString()
                }

                // print out details of each message
                println("Message #" + (i + 1) + ":")
                println("\t From: $from")
                println("\t Subject: $subject")
                println("\t Sent Date: $sentDate")
                println("\t Message: $messageContent")
                println("\t Attachments: $attachFiles")
            }

            // disconnect
            folderInbox.close(false)
            store.close()
        } catch (ex: NoSuchProviderException) {
            println("No provider for imap.")
            ex.printStackTrace()
        } catch (ex: MessagingException) {
            println("Could not connect to the message store")
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun parseDate(text: String) {
        val dateStart = text.lines().map {
            it.contains("DTSTART") to it
        }.filter { (contains, _) -> contains }
            .map { it.second }

        val dateEnd = text.lines().map {
            it.contains("DTEND") to it
        }.filter { (contains, _) -> contains }
            .map { it.second }

        println(dateStart[2])
        println(dateEnd[0])
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val host = "imap.gmail.com"
            val port = "993"
            val userName = ""
            val password = ""
            val receiver = EmailAttachmentReceiver()
            receiver.downloadEmailAttachments(host, port, userName, password)
        }
    }
}