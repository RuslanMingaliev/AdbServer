package com.kaspersky.test_server.implementation

import com.kaspersky.test_server.api.CommandHandler
import com.kaspersky.test_server.api.ConnectionServer
import com.kaspersky.test_server.api.ExecutorResult
import com.kaspersky.test_server.implementation.transferring.MessagesListener
import com.kaspersky.test_server.implementation.transferring.ResultMessage
import com.kaspersky.test_server.implementation.transferring.SocketMessagesTransferring
import com.kaspersky.test_server.implementation.transferring.TaskMessage
import com.kaspresky.test_server.log.Logger
import java.net.Socket
import java.util.concurrent.Executors

// todo logs, comments
internal class ConnectionServerImplBySocket(
    private val socket: Socket,
    private val commandHandler: CommandHandler<ExecutorResult>,
    private val logger: Logger
) : ConnectionServer {

    private var connectionMaker: ConnectionMaker = ConnectionMaker()
    private val socketMessagesTransferring: SocketMessagesTransferring<TaskMessage, ResultMessage<ExecutorResult>> =
        SocketMessagesTransferring.createTransferring(socket)
    // todo change cache pool
    private val backgroundExecutor = Executors.newCachedThreadPool()

    // todo think about @Synchronized
    @Synchronized
    override fun connect() {
        connectionMaker.connect {
            handleMessages()
        }
    }

    private fun handleMessages() {
        socketMessagesTransferring.startListening(object : MessagesListener<TaskMessage> {
            override fun listenMessages(receiveModel: TaskMessage) {
                backgroundExecutor.execute {
                    val result = commandHandler.complete(receiveModel.command)
                    socketMessagesTransferring.sendMessage(
                        ResultMessage(receiveModel.command, result)
                    )
                }
            }
        })
    }

    // todo think about @Synchronized
    @Synchronized
    override fun disconnect() {
        connectionMaker.disconnect {
            socketMessagesTransferring.stopListening()
            socket.close()
        }
    }

}