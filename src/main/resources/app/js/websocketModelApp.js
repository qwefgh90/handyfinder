var app = angular.module('websocketModelApp', []);
//app.constant('sockJsProtocols', ["xhr-streaming", "xhr-polling"]); // only allow XHR protocols
app.constant('sockJsProtocols', []);
app.factory("StompClient", ['sockJsProtocols', '$q',
function(sockJsProtocols, $q) {
	var stompClient;
	var wrappedSocket = {
		init : function(url) {
			if (sockJsProtocols.length > 0) {
				stompClient = Stomp.over(new SockJS(url, null, {
					transports : sockJsProtocols
				}));
			} else {
				stompClient = Stomp.over(new SockJS(url));
			}
		},
		connect : function() {
			var deferred = $q.defer();
			if (!stompClient) {
				reject("STOMP client not created");
			} else {
				stompClient.connect({}, function(frame) {
					deferred.resolve(frame);
				}, function(error) {
					deferred.reject("STOMP protocol error " + error);
				});
			}
			return deferred.promise;
		},
		disconnect : function() {
			stompClient.disconnect();
		},
		subscribe : function(destination) {
			var deferred = $q.defer();
			if (!stompClient) {
				deferred.reject("STOMP client not created");
			} else {
				stompClient.subscribe(destination, function(message) {
					deferred.notify(JSON.parse(message.body));
				});
			}
			return deferred.promise;
		},
		send : function(destination, headers, object) {
			stompClient.send(destination, headers, object);
		}
	};
	return wrappedSocket;

}]);

app.factory('ProgressService', ['StompClient', '$q',
function(stompClient, $q) {
	return {
		connect : function() {
			stompClient.init("/endpoint");
			return stompClient.connect();
		},
		disconnect : function() {
			stompClient.disconnect();
		},
		subProgress : function() {
			return stompClient.subscribe("/progress/single");
		},
		sendStartIndex : function() {
			return stompClient.send("/handyfinder/command/index/start", {}, '');
			// JSON.stringify(tradeOrder));
		},
		sendStopIndex : function() {
			return stompClient.send("/handyfinder/command/index/stop", {}, '');
			//JSON.stringify(tradeOrder));
		}
	};

}]);

app.factory('GUIService', ['StompClient', '$q',
function(stompClient, $q) {
	return {
		connect : function() {
			stompClient.init("/endpoint");
			return stompClient.connect();
		},
		disconnect : function() {
			stompClient.disconnect();
		},
		subDirectory : function() {
			return stompClient.subscribe("/gui/directory");
		}
		/*,
		// openDirectory : function() {
			return stompClient.send("/handyfinder/command/gui/directory", {}, '');
			// JSON.stringify(tradeOrder));
		}*/
	};

}]);

