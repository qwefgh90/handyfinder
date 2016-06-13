var app = angular.module('indexModelApp', []);
app.factory("SearchModel",['$rootScope', function($rootScope){
	var service = {
		model : {
			searchKeyword : '',
			searchResult : [],
			searchCount : 0,
		},

        SaveState: function () {
            sessionStorage.userService = angular.toJson(service.model);
        },

        RestoreState: function () {
            service.model = angular.fromJson(sessionStorage.userService);
        }

        // $rootScope.$on("savestate", service.SaveState);
        // $rootScope.$on("restorestate", service.RestoreState);

	};
	return service;
}]);


app.factory("OptionModel",['$rootScope', function($rootScope){
	var service ={
		model : {
			maximum_size : 5,
		}
	};
	return service;
}]);

app.factory("IndexModel",['$rootScope', function($rootScope){
	var service = {
			model : {
				auto_update_index : false,
				auto_clean_files : false,
				supportTypes : [],
				pathList : [],
				index_manager_status : {
						open : true
					},
				index_option_status : {
							open : true
					},
				index_progress_status : {
							open : false,
							progress : false,
							progressItemCount : 0,
							progressBarVisible : false,
							alerts : [{// 0
								open : false,
								type : 'success',
								msg : 'Directories are stored on disk!',
								timeout : 10000
							}, {// 1
								open : false,
								type : 'danger',
								msg : 'Storing directories is failed on disk!',
								timeout : 10000
							}, {// 2
								open : false,
								type : 'success',
								msg : 'Directories are loaded on disk!',
								timeout : 10000
							}, {// 3
								open : false,
								type : 'danger',
								msg : 'Loading directories is failed from disk!',
								timeout : 10000
							}, {// 4
								open : false,
								type : 'success',
								msg : 'Ready to search your files!',
								timeout : 10000
							}],
							alertQ : [],
							addAlertQ : function(index) {
								if (this.alertQ.indexOf(this.alerts[index]) != -1)	//already added
									return;
								this.alertQ.push(this.alerts[index]);
								this.progressItemCount++;
								this.refreshState();
							},
							removeAlertQ : function(queueSeq) {
								if (this.alertQ.length <= queueSeq)	//invalid request
									return;
								this.alertQ.splice(queueSeq, 1);
								this.progressItemCount--;
								this.refreshState();
							},
							refreshState : function() {
								if (this.progressItemCount > 0 || this.progressBarVisible == true) {
									this.progress = true;
									this.open = true;
								} else {
									this.progress = false;
									this.open = false;
								}
							}
					},
				processIndex : 0,
				totalProcessCount : 100,
				processPath : '',
				state : 'TERMINATE'
			},

	        SaveState: function () {
	            sessionStorage.userService = angular.toJson(service.model);
	        },

	        RestoreState: function () {
	            service.model = angular.fromJson(sessionStorage.userService);
	        }

	        // $rootScope.$on("savestate", service.SaveState);
	        // $rootScope.$on("restorestate", service.RestoreState);

		};
		return service;
	
}]);

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
app.factory("Document", function() {
	// Define the constructor function.
	function Document(createdTime, modifiedTime, title, pathString, contents, parentPathString) {
		/*
		 * private long createdTime; private long modifiedTime; private String
		 * title; private String pathString; private String contents;
		 */
		this.createdTime = createdTime;
		this.modifiedTime = modifiedTime;
		this.title = title;
		this.pathString = pathString;
		this.contents = contents;
		this.parentPathString = parentPathString;
	}

	// Define the "instance" methods using the prototype
	// and standard prototypal inheritance.
	Document.prototype = {
	};
	// Define the "class" / "static" methods. These are
	// utility methods on the class itself; they do not
	// have access to the "this" reference.
	// Path.createInstance = function(path) {
	// return new Path(path, true, true);
	// };
	// Return constructor - this is what defines the actual
	// injectable in the DI framework.
	return (Document);
}); 