if(typeof console == 'undefined') {
	var console = {log: function() {}};
}

$(document).ready(function() {
	$('#username').focus();

	var showError = function(msg) {
		if(msg) {
			$('#loginerror').text(msg);
			$('#loginerror').css('display', '');
		} else {
			$('#loginerror').css('display', 'none');
		}
	};
	var showMsg = function(msg) {
		if(msg) {
			$('#loginmsg').text(msg);
			$('#loginmsg').css('display', '');
		} else {
			$('#loginmsg').css('display', 'none');
		}
	};

	$('#login_form').submit(function(e) {
		e.preventDefault();
		var params = {
			'username': $('#username').val(),
			'password': $('#password').val(),
		};
		$.post('/login', params, function(data) {
			$('#username').val('');
			$('#password').val('');
			if(data.error) {
				showError(data.error);
				$('#username').focus();
			} else {
				$('#step1').css('display', 'none');
				$('#step2').css('display', '');
				onLogin(data.session_key);
			}
		}, 'json');
	});

	$('#signup_btn').click(function(e) {
		e.preventDefault();
		var params = {
			'username': $('#username').val(),
			'password': $('#password').val(),
		};
		$.post('/signup', params, function(data) {
			$('#username').val('');
			$('#password').val('');
			if(data.error) {
				showError(data.error);
			} else {
				showMsg('Your account has been created. You can now login below.');
			}
			$('#username').focus();
		}, 'json');
	});

	$('#username').keypress(function() {
		if($('#username').val().length > 0) {
			showError();
			showMsg();
		}
	});
});

var onLogin = function(sessionKey) {
	$.post('/version', function(data) {
		if(data.error) {
			$('#versiondiv').text('Error: ' + data.error + '.');
			$('#versiondiv').css('display', '');
		} else if(!data.up_to_date) {
			$('#versiondiv').text('A new version is available! Download it at https://entgaming.net/wc3connect');
			$('#versiondiv').css('display', '');
		}
	}, 'json');

	$.post('/motd', function(data) {
		if(data.motd) {
			$('#motddiv').text(data.motd);
			$('#motddiv').css('display', '');
		}
	}, 'json');

	$.post('/get-config', function(data) {
		if(data.no_broadcast) {
			$('#cfgNoBroadcast').prop('checked', true);
		}
	}, 'json');

	var setGames = function(tbody, games) {
		var existingRows = {};
		tbody.children('tr.game').each(function() {
			var uiRow = $(this);
			var key = uiRow.data('key');
			existingRows[key] = uiRow;
		});
		var lastRow = null;
		for(var gameIdx = 0; gameIdx < games.length; gameIdx++) {
			var game = games[gameIdx];

			if(game.app_game) {
				var cols = [
					game.app_game.location,
					game.gamename,
					game.app_game.map,
					game.app_game.host,
					game.app_game.slots_taken + '/' + game.app_game.slots_total,
				];
			} else {
				var cols = [
					game.location,
					game.gamename,
					game.map,
					'',
					'',
				];
			}
			var key = game.ip + '.' + game.gamename;
			var existingRow = existingRows[key];
			if(existingRow) {
				existingRow.data('uid', game.uid);
				for(var i = 0; i < cols.length; i++) {
					var el = cols[i];
					if(existingRow.children().eq(i).text() != el) {
						existingRow.children().eq(i).text(el);
					}
				}
				if(game.app_game) {
					existingRow.data('gameid', game.app_game.id);
				}
				lastRow = existingRow;
				delete existingRows[key];
			} else {
				var uiRow = $('<tr>')
					.addClass('game')
					.data('key', key)
					.data('uid', game.uid);
				for(var i = 0; i < cols.length; i++) {
					var uiCol = $('<td>').text(cols[i]);
					uiRow = uiRow.append(uiCol);
				}
				if(game.app_game) {
					uiRow = uiRow.data('gameid', game.app_game.id);
				}
				if(lastRow) {
					lastRow.after(uiRow);
					lastRow = uiRow;
				} else {
					tbody.prepend(uiRow);
					lastRow = uiRow;
				}
			}
		}
		for(var key in existingRows) {
			existingRows[key].remove();
		}
	};

	var getGames = function() {
		$.post('/games', function(data) {
			setGames($('#public-tbody'), data.publicGames);
			setGames($('#autohost-tbody'), data.autohostGames);
			setGames($('#others-tbody'), data.otherGames);
		}, 'json');
	};
	$('tbody').on('click', 'tr.game', function() {
		$('tr.game.selected').removeClass('selected');
		$('tr.gameinfo').remove();
		$(this).addClass('selected');
		var uid = $(this).data('uid');
		$.post('/show', {'uid': uid});

		if($(this).data('gameid')) {
			console.log('adding stuff');
			var row = $('<tr>')
				.addClass('gameinfo')
				.data('gameid', $(this).data('gameid'));
			var col = $('<td>')
				.addClass('gameinfo')
				.attr('colspan', 5);
			row = row.append(col);
			$(this).after(row);
			refreshInfo();
		}
	});
	$('tbody').on('click', 'tr.gameinfo a', function(e) {
		e.preventDefault();
	});
	var refreshInfo = function() {
		var row = $('tr.gameinfo');
		if(row.length == 0) {
			return;
		}
		var gameid = row.data('gameid');
		$.post('/gameinfo', {'gameid': gameid}, function(data) {
			row.children().html(data);
		});
	};
	getGames();
	setInterval(function() {
		getGames();
	}, 1000);
	setInterval(function() {
		refreshInfo();
	}, 3000);

	var setConfig = function() {
		var params = {
			'no_broadcast': $('#cfgNoBroadcast').prop('checked') ? 'yes' : '',
		};
		$.post('/set-config', params);
	};

	$('#cfgNoBroadcast').change(function() {
		setConfig();
	});

	var showSettingsInfo = function(msg) {
		$('#settingsinfo').text(msg);
		$('#settingsinfo').css('display', '');
		setTimeout(function() {
			$('#settingsinfo').css('display', 'none');
		}, 5000);
	};

	$('#validate_btn').click(function(e) {
		e.preventDefault();
		$('#settingsinfo').css('display', 'none');
		var params = {
			key: $('#validate_key').val(),
		};
		$('#validate_key').val('');
		$.post('/validate', params, function(data) {
			if(data.error) {
				showSettingsInfo(data.error);
			} else {
				showSettingsInfo('The validation request was sent successfully! If the key is correct, your WC3Connect account should now be validated in your entgaming.net account.');
			}
		}, 'json');
		$('#validateModal').modal('hide')
	});
};
