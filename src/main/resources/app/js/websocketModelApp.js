var app = angular.module('websocketModelApp', []);
//app.constant('sockJsProtocols', ["xhr-streaming", "xhr-polling"]); // only allow XHR protocols
app.constant('sockJsProtocols', []);
app.factory("StompClient", ['sockJsProtocols', '$q',
function(sockJsProtocols, $q) {
	function StompClient(){
		this.stompClient = undefined;
	};
	
	StompClient.prototype = {
		init : function(url) {
			if (sockJsProtocols.length > 0) {
				this.stompClient = Stomp.over(new SockJS(url, null, {
					transports : sockJsProtocols
				}));
			} else {
				this.stompClient = Stomp.over(new SockJS(url));
			}
		},
		connect : function() {
			var deferred = $q.defer();
			if (!this.stompClient) {
				reject("STOMP client not created");
			} else {
				this.stompClient.connect({}, function(frame) {
					deferred.resolve(frame);
				}, function(error) {
					deferred.reject("STOMP protocol error " + error);
				});
			}
			return deferred.promise;
		},
		disconnect : function() {
			this.stompClient.disconnect();
		},
		subscribe : function(destination) {
			var deferred = $q.defer();
			if (!this.stompClient) {
				deferred.reject("STOMP client not created");
			} else {
				this.stompClient.subscribe(destination, function(message) {
					deferred.notify(JSON.parse(message.body));
				});
			}
			return deferred.promise;
		},
		send : function(destination, headers, object) {
			this.stompClient.send(destination, headers, object);
		},
		heartbeat : function(){
			var deferred = $q.defer();
			this.stompClient.onheartbeat = function(){
				deferred.resolve();
			}
			return deferred.promise;
		}
	};
	return (StompClient);

}]);

app.factory('ProgressService', ['StompClient', '$q','$log',
function(StompClient, $q, $log) {
	var stompClient = new StompClient();
	return {
		connect : function() {
			stompClient.init("/endpoint");
			stompClient.heartbeat().then(function(){$log.log('progress service heartbeat');},function(){},function(){})
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

app.factory('GUIService', ['StompClient', '$q','$log',
function(StompClient, $q, $log) {
	var stompClient = new StompClient();
	return {
		connect : function() {
			stompClient.init("/endpoint");
			stompClient.heartbeat().then(function(){$log.log('gui service heartbeat');},function(){},function(){})
			return stompClient.connect();
		},
		disconnect : function() {
			stompClient.disconnect();
		},
		subDirectory : function() {
			return stompClient.subscribe("/gui/directory");
		}
		,
		openDirectory : function(path) {
			stompClient.send("/handyfinder/command/gui/open", {}, JSON.stringify({path:path}));
		}
	};

}]);

