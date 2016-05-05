var app = angular.module('indexModelApp', [])
app.factory("Friend", function() {
	// Define the constructor function.
	function Friend(firstName, lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	// Define the "instance" methods using the prototype
	// and standard prototypal inheritance.
	Friend.prototype = {
		getFirstName : function() {
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
	function Path(path, used) {
		this.path = path;
		this.used = used;
	}

	// Define the "instance" methods using the prototype
	// and standard prototypal inheritance.
	Path.prototype = {
		getPath : function() {
			return (this.path);
		},
		getUsed : function() {
			return (this.used);
		}
	};
	// Define the "class" / "static" methods. These are
	// utility methods on the class itself; they do not
	// have access to the "this" reference.
	Path.createInstance = function(path) {
		return new Path(path, true);
	};
	// Return constructor - this is what defines the actual
	// injectable in the DI framework.
	return (Path);
}); 