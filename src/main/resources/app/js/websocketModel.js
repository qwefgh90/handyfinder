var app = angular.module('websocketModel', [])
app.factory("Friend", function() {
	// Define the constructor function.
	function Friend(firstName, lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	// Define the "instance" methods using the prototype
	// and standard prototypal inheritance.
	Friend.prototype = {
		connect : function() {
	
			var socket = new SockJS("/endpoint");
			var stompClient = Stomp.over(socket);

			stompClient.connect({}, function(frame) {

				console.log('Connected: ' + frame);
				stompClient.subscribe('/receiver/progress', function(greeting) {
					alert(greeting);
				});
				stompClient.send("/websocket/command", {}, JSON.stringify({
					'command' : "1"
				}));
			});

			
			return (this.firstName );
		},
		getFullName : function() {
			return (this.firstName + " " + this.lastName );
		}
	};
	// Define the "class" / "static" methods. These are
	// utility methods on the class itself; they do not
	// have access to the "this" reference.
	Friend.fromFullName = function(fullName) {
		var parts = fullName.split(/\s+/gi);
		return (new Friend(parts[0], parts.splice(0, 1) && parts.join(" "))
		);
	};
	// Return constructor - this is what defines the actual
	// injectable in the DI framework.
	return (Friend );
});

app.factory("Path", function() {
	// Define the constructor function.
	function Path(path, used, recursively) {
		this.pathString = path;
		this.used = used;
		this.recursively = recursively;
	}

	// Define the "instance" methods using the prototype
	// and standard prototypal inheritance.
	Path.prototype = {
		getPathString : function() {
			return (this.pathString);
		},
		getUsed : function() {
			return (this.used);
		},
		getRecursively : function() {
			return (this.recursively);
		}
	};
	// Define the "class" / "static" methods. These are
	// utility methods on the class itself; they do not
	// have access to the "this" reference.
	Path.createInstance = function(path) {
		return new Path(path, true, true);
	};
	// Return constructor - this is what defines the actual
	// injectable in the DI framework.
	return (Path);
});
