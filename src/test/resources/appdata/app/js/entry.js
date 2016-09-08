require.config({
	
	/* short name of file path */
	paths: {
		sockjs : '../bower_components/sockjs-client/dist/sockjs-0.3.4.min',
		stomp : '../bower_components/stomp-websocket/lib/stomp.min',
		angular : '../bower_components/angular/angular.min',
		angularRoute : '../bower_components/angular-route/angular-route.min',
		angularAnimate: '../bower_components/angular-animate/angular-animate.min',
		ngContextmenu : '../bower_components/ng-contextmenu/dist/ng-contextmenu',
		angularBootstrap : '../bower_components/angular-bootstrap/ui-bootstrap-tpls.min',
		angularSanitize : '../bower_components/angular-sanitize/angular-sanitize.min'
	},
	
	/* (key: module name , exports: global variable) for non-AMD(Asynchronous Module Definition) */
	shim: {
		angular: {exports: 'angular'},
		stomp: {deps: ['sockjs'], exports: 'Stomp'},
        angularRoute: {deps: ['angular'], exports: 'angular'},
		angularAnimate: {deps: ['angular'], exports: 'angular'},
		ngContextmenu : {deps: ['angular'], exports: 'angular'},
        angularBootstrap: {deps: ['angular','angularAnimate'], exports: 'angular'},
		angularSanitize: {deps: ['angular'], exports: 'angular'}
	}	
});
/* short or full name of script path */
require(['angular', 'angularRoute', 'angularAnimate', 'ngContextmenu' ,'angularBootstrap'
         ,'angularSanitize', 'sockjs', 'stomp'
         , './handyfinderwebapp', './indexModel', './webSocketModel', './searchModel', './optionModel'], function(angular){
    
	angular.bootstrap(document, ['handyfinderwebapp']);
});
