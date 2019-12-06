#include <cstdio>
#include "sys/time.h"
#include "unistd.h"
#include <netinet/in.h>
#include <cstring>
#include <cstdlib>
#include <sys/types.h>

#ifndef  __USE_GNU
#define _GNU_SOURCE
#define __USE_GNU
#endif
#include <sys/socket.h>

#include <sys/select.h>
#include <arpa/inet.h>

// Constants used by this program
const int SERVER_PORT = 3005;
const int BUFFER_LENGTH = 2048;
const int MAX_NUM_CONNECTION = 10;

int make_server_sock (int16_t port) {
	struct sockaddr_in serveraddr;
	int server_sock_fd = -1;
	const int optval = true;
	do {
		/*
		* The socket() function returns a socket descrpitor,
		* which represents an endpoint.
		* Tje statement also identifies that the INET (Internet Protocol)
		* address family with the TCP transport (SOCK_STREAM) will be used
		* for this socket
		*/
		// create a non-blocking TCP/IP socket
		server_sock_fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
		if (server_sock_fd < 0) {
			perror("socket() failed");
			break;
		}

		/*
		* The setsockopt() function is used to allow the local address
		* to be reused when the server is restarted before the required
		* wait time expires.
		*/
		if (setsockopt(server_sock_fd, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval)) < 0) {
			perror("setsockopt (SO_REUSEADDR) failed");
			break;
		}

		/*
		* After the socket descriptor is created, a bind() function gets a
		* unique name for the socket. In this example, the user sets the
		* s_addr to zero, which allows connections to be established from any
		* client that specifies port 3005.
		*/

		/* Give the socket a unique name. */
		memset(&serveraddr, 0, sizeof(serveraddr));
		serveraddr.sin_family = AF_INET;
		serveraddr.sin_port = htons(port);
		serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);

		if (bind(server_sock_fd, (struct sockaddr *) &serveraddr, sizeof(serveraddr))) {
			perror("bind() failed");
			break;
		}
		return server_sock_fd;
	} while (false);
	close(server_sock_fd);
	return server_sock_fd;
}

bool echo_client (int client_sock_fd) {
	// Receive data from the client into buffer
	static thread_local char buffer[BUFFER_LENGTH];
	static thread_local int result_code = -1;
	do {
		result_code = recv(client_sock_fd, buffer, sizeof(buffer), 0);
		if (result_code < 0) {
			perror("recv() failed");
			break;
		}
		printf("%d bytes of data were received", result_code);
		if (result_code == 0 || result_code < sizeof(buffer)) {
			printf("The client closed the connection before all of the data was sent\n");
			break;
		}
		// Echo the data back to the client
		result_code = send(client_sock_fd, buffer, sizeof(buffer), 0);
		if (result_code < 0) {
			perror("send() failed");
			break;
		}
		return false;
	} while (false);
	return true;
}

int main () {
	// Variable and structure definition
	int client_sock_fd = -1;
	int result_code = -1;
	struct sockaddr_in client_address;
	socklen_t client_address_size = sizeof(client_address);
	fd_set active_fd_set, read_fd_set;

	const auto server_sock_fd = make_server_sock(SERVER_PORT);

	/*
	 * A do/while (False) loop is used to make error cleanup easier.
	 * The close() of each of the socket descriptor is only done once
	 * at the vary end of the program
	 */
	do {
		if (server_sock_fd < 0) {
			break;
		}

		/*
		 * The listen() function allows the server to accept incoming client connections.
		 * In this example, the backlog is set to 10.
		 * This means that the system will queue 10 incoming connection requests
		 * before the system starts rejecting the incoming requests.
		 */
		result_code = listen(server_sock_fd, MAX_NUM_CONNECTION);
		if (result_code < 0) {
			perror("listen() failed");
			break;
		}
		printf("Ready for client connect().\n");

		/* Initialize the set of active sockets. */
		FD_ZERO(&active_fd_set);
		FD_SET(server_sock_fd, &active_fd_set);

		/*
		 * Main loop: wait for connection request or stdin command
		 * If connection, the echo back and close connection
		 */
		while (true) {
			/* Block until input arrives on one or more active sockets.
			* The select() function allows the process to wait for an event to
			* occur and to wake up the process when the event occurs. In this
			* exmaple, the system notifies the process only when data is available
			* to read. a tv_sec second is used on this select call.
			*/
			read_fd_set = active_fd_set;
			// nfds is the highest-numbered file descriptor in any of the three sets, plus 1.
			result_code = select(FD_SETSIZE, &read_fd_set, NULL, NULL, NULL);
			if (result_code < 0) {
				perror("select() failed");
				break;
			}
			if (result_code == 0) {
				printf("select() timed out.\n");
				continue;;
			}

			// Service all the sockets with input pending
			for (int i = 0; i < FD_SETSIZE; ++i) {
				if (FD_ISSET(i, &read_fd_set)) {
					if (i == server_sock_fd) {
						/* 
						* Connection request on original socket.
						* The server uses the accept() functin to accept an incoming
						* connection request.
						*/
						client_sock_fd = accept4(server_sock_fd, (struct sockaddr *) &client_address, &client_address_size,SOCK_NONBLOCK);
						if (client_sock_fd < 0) {
							perror("accept() failed");
							break;
						}
						fprintf(stderr, "Server: connect from host %s, port %hd.\n",
						        inet_ntoa(client_address.sin_addr),
						        ntohs(client_address.sin_port));
						FD_SET(client_sock_fd, &active_fd_set);
					} else {
						/* Data arriving on an already-connected socket. */
						client_sock_fd = i;
						if (echo_client(client_sock_fd)) {
							close(i);
							FD_CLR(i, &active_fd_set);
						}
					}
				}
			}

		}
	} while (false);

	// Close down any open socket descriptors   
	close(server_sock_fd);
	return 0;
}
