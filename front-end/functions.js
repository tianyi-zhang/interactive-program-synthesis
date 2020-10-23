$(document).ready(function(){
    $('[data-toggle="tooltip"]').tooltip({
        trigger : 'hover'
    });    
    var actions = '<a class="add" title="Add" data-toggle="tooltip"><i class="material-icons">&#xE03B;</i></a>' + 
        '<a class="edit" title="Edit" data-toggle="tooltip"><i class="material-icons">&#xE254;</i></a>' + 
        '<a class="delete" title="Delete" data-toggle="tooltip"><i class="material-icons">&#xE872;</i></a>';
    // Append table with add row form on add new button click
    $(".add-new").click(function(){
        $(this).attr("style", "display:none");
        var index = $("table#examples tbody#task-" + cur_task + " tr:last-child").index();
        var row = '<tr>' +
            '<td><input type="text" class="form-control" name="input string" id="input-example"></td>' +
            '<td class="icon"><select id="output-example" class="form-control"><option selected>Accept</option><option>Reject</option></select></td>' +
            '<td class="icon">' + actions + '</td>' +
        '</tr>';
        $("table#examples tbody#task-" + cur_task).append(row);     
        $("table#examples tbody#task-" + cur_task + " tr").eq(index + 1).find(".add, .edit").toggle();
        $('[data-toggle="tooltip"]').tooltip();
    });
    // Add row on add button click
    $(document).on("click", "table .add", function(){
        var empty = false;
        var input = $(this).parents("tr").find('input[type="text"]');
        input.each(function(){
            $(this).parent("td").html($(this).val());
        });

        var output = $(this).parents("tr").find('select');
        output.each(function(){
            if($(this).val() == "Accept") {
                $(this).parent("td").html('<i class="material-icons match">check</i>');
            } else {
                $(this).parent("td").html('<i class="material-icons unmatch">close</i>');
            }
        });

        $(this).parents("tr").find(".add, .edit").toggle();
        $(this).parents("tr").find(".move").removeAttr("style");
        $(".add-new").removeAttr("style");    
    });
    // Edit row on edit button click
    $(document).on("click", "table#examples .edit", function(){        
        $(this).parents("tr").find("td:not(:last-child)").each(function(){
            if($(this).hasClass('icon')) {
                var s = '<select id="output-example" class="form-control"><option selected>';
                if($(this).text() == 'check') {
                    s += 'Accept' + '</option><option>' + 'Reject';
                } else {
                    s += 'Reject' + '</option><option>' + 'Accept';
                }
                s+= '</option></select>';

                $(this).html(s);
            } else {
                $(this).html('<input type="text" class="form-control" value="' + $(this).text() + '">');
            }
        });     
        $(this).parents("tr").find(".add, .edit").toggle();
        $(".add-new").attr("style", "display:none");
    });
    // Delete row on delete button click
    $(document).on("click", ".delete", function(){
        $(this).parents("tr").remove();
        $(".add-new").removeAttr("style");

        // remove any tooltips
        $('div.tooltip').remove();
    });

    $(document).on("click", "table#synthetic .move", function(){
        var input = $(this).parents("tr").find("td:first-child").text();
        var output = $(this).parents("tr").find("td:nth-child(2)").html();
        var row = '<tr>' +
            '<td>' + escapeHtml(input) + '</td>' +
            '<td class="icon">' + output + '</td>' +
            '<td class="icon">' + actions + '</td>' +
            '</tr>';
        $("table#examples tbody#task-" + cur_task).append(row);
        $('[data-toggle="tooltip"]').tooltip();
        $(this).parents("tr").remove();
    });

    $(document).on("click", "table#synthetic .edit", function(){        
        $(this).parents("tr").find("td:not(:last-child)").each(function(){
            if($(this).hasClass('icon')) {
                var s = '<select id="output-example" class="form-control"><option selected>';
                if($(this).text() == 'check') {
                    s += 'Accept' + '</option><option>' + 'Reject';
                } else {
                    s += 'Reject' + '</option><option>' + 'Accept';
                }
                s+= '</option></select>';

                $(this).html(s);
            } else {
                $(this).html('<input type="text" class="form-control" value="' + $(this).text() + '">');
            }
        });

        $(this).parents("tr").find(".move").attr("style", "display:none");
        $(this).parents("tr").find(".add, .edit").toggle();
    });
});

window.onbeforeunload = function(){
   socket.send("Window closed");
}

var cur_task = "0";

function changeTask() {
    var task_id = $('#change-task').val();
    cur_task = task_id;
    $('div#task-desc span').each(function() {
        var attr = $(this).attr('style');
        if (typeof attr !== typeof undefined && attr !== false) {
            if ($(this).attr('id') == "task-" + task_id) {
                $(this).removeAttr('style');
            }
        } else {
            $(this).attr('style', 'display:none');
        }
    });

    // clean synthesized examples and regexes of the previous task (if any)
    $('div#user-annotations span').remove();
    $('div#user-annotations button').remove();
    $('#regex-container div.regex').remove();
    $('#synthetic tr').remove();
    // also remove the slider
    $('div#slider-container').empty();
    // reset the synthesis progress bar
    $('.progress .progress-bar').attr('class', 'progress-bar progress-bar-striped active');
    $('.progress .progress-bar').attr('style', 'width: 0%;');
    $('.progress .progress-bar').text('0%');
    
    // show some initial examples
    $('table#examples tbody').each(function() {
        var attr = $(this).attr('style');
        if (typeof attr !== typeof undefined && attr !== false) {
            if ($(this).attr('id') == "task-" + task_id) {
                $(this).removeAttr('style');
            }
        } else {
            $(this).attr('style', 'display:none');
        }
    });

    // reset all global variables
    generalizations = {};
    synthetic_examples = {};
    buttonId = 1;
    generalized_chars = null;
    percent = 0;
    isComplete = false;
    isTimeout = -1;
    sel_regex = null;

    // send a signal to the server
    socket.send("Reset");
}

var buttonId = 1;

var generalized_chars;

function highlightSelection(clr) {
    var selection;

    //Get the selected stuff
    if (window.getSelection)
        selection = window.getSelection();
    else if (typeof document.selection != "undefined")
        selection = document.selection;

    //Get a the selected content, in a range object
    var range = selection.getRangeAt(0);
    var selection_str = selection.toString();
    var is_safari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

    //If the range spans some text, and inside a tag, set its css class.
    if (range && !selection.isCollapsed) {
        if (selection.anchorNode.parentNode == selection.focusNode.parentNode || is_safari) {
            var span = document.createElement('span');
            if(clr == 'include' || clr == 'exact-match') {
            	span.className = 'must-have';
                generalized_chars = null;
            } else if (clr == 'exclude') {
            	span.className = 'not-have';
                generalized_chars = null;
            } else if (clr == 'char-family') {
            	span.className = 'char-family';
                generalized_chars = escapeHtml(selection_str);
            }
            
            range.surroundContents(span);

            // if the user annotates a regex, add the annotated subexpression in the breadcrumb
            if(clr == 'include' || clr == 'exclude') {
                var breadcrumb = $('#user-annotations');
                var label = '<span class="badge badge-pill annotation ' + span.className + '">' + escapeHtml(selection_str) + '</span>' 
                    + '<button id="' + buttonId + '" class="removable" onclick="removeAnnotation(this.id)">x</button>';
                breadcrumb.append(label);
                buttonId++;
            }
        }
    }
}

function addNewExample() {
    var input = document.getElementById('input-example');
    var output = document.getElementById('output-example');

    document.createElement('tr');
}

var socket; 

// Handle any errors that occur.
const socketErrorListener = (event) => {
    console.log('WebSocket Error: ' + error);
};

// Make sure we're connected to the WebSocket before trying to send anything to the server
const socketOpenListener = (event) => {
    // send the code example to the backend for parsing and analysis
    console.log('Connected to the server.');
}

// Handle messages sent by the backend.
const socketMessageListener = (event) => {
    console.log(event.data);
    if (event.data.startsWith("regexes:")) {
        // Display the new synthesized regexes
        displayRegex(event.data.substring(event.data.indexOf(":") + 2));
    } else if (event.data.startsWith("examples:")) {
        // Store the generated examples
        var message = event.data.substring(event.data.indexOf(":") + 2);
        var selected_regexes = getSelectedRegexes();
        synthetic_examples[selected_regexes] = message;

        // display similar examples by default
        if(isDisplayWildExamples) {
            displaySyntheticExamples(message.split("\n")[1], true);
        } else {
            displaySyntheticExamples(message.split("\n")[0], false);
        }
    }
}

// Show a disconnected message when the WebSocket is closed.
const socketCloseListener = (event) => {
    if(socket) {
        console.error('Disconnected.');
    }
    // socket = new WebSocket('ws://3.236.11.113:8070/');
    socket = new WebSocket('ws://127.0.0.1:8080/');
    socket.addEventListener('open', socketOpenListener);
    socket.addEventListener('message', socketMessageListener);
    socket.addEventListener('close', socketCloseListener);
    socket.addEventListener('error', socketErrorListener);
};

// establish the connection to server
socketCloseListener();

// set global variables for the synthesis progress bar
var timerId;
var percent = 0;
var isComplete = false;
var isTimeout = -1;  // -1: not known; 0: timeout; 1: complete

function synthesize() {
    // reset the signals
    percent = 0;
    isComplete = false;
    isTimeout = -1;

    // Gather input examples and their annotations
    var rows = $("#examples tbody#task-" + cur_task +" tr");
    var examples = '[';
    rows.each(function() {
        var input = $(this).children('td').eq(0);
        var output = $(this).children('td').eq(1);

        if(input.find('input').length || output.find('select').length){
            return;
        }

        examples += '{ "input" : "' + escapeJson(input.text()) + '", ' + '"exact" : [';
        if(input.children('.must-have').length > 0 && output.text() == 'check') {
            // marked as a literal on a positive example
            input.children('.must-have').each(function() {
                examples += '"' + escapeJson($(this).text()) + '", ';
            });
            examples = examples.substring(0, examples.length - 2);
        } 
        examples += '], "unmatch" : [';

        if(input.children('.must-have').length > 0 && output.text() == 'close') {
            input.children('.must-have').each(function() {
                examples += '"' + escapeJson($(this).text()) + '", ';
            });
            examples = examples.substring(0, examples.length - 2);
        }
        examples += '], "generalize" : [';

        if(input.children('.char-family').length > 0) {
            input.children('.char-family').each(function() {
                var text = $(this).text();
                examples += '"' + escapeJson(text) + '@@@' + generalizations[text] + '", ';
            });
            examples = examples.substring(0, examples.length - 2);
        }
        examples += '], ';

        examples += '"output" : ';
        if(output.text() == "check") {
            examples += 'true }';
        } else if (output.text() == "close") {
            examples += 'false }';
        }
        examples += ", "
    });
    
    if(examples.length > 0) {
        examples = examples.substring(0, examples.length - 2);
    }
    examples += "]";

    // Gather snthesized programs and their annotations if any
    var regexes = '[';
    var includes = '[';
    var breadcrumbs = $("div#user-annotations span.must-have");
    breadcrumbs.each(function() {
        var include = unescapeHtml($(this).text());
        includes += '"' + include + '", ';
    });
    if(breadcrumbs.length > 0) {
        includes = includes.substring(0, includes.length - 2);
    }
    includes += ']';
    var excludes = '[';
    breadcrumbs = $("div#user-annotations span.not-have");
    breadcrumbs.each(function() {
        var exclude = unescapeHtml($(this).text());
        excludes += '"' + exclude + '", ';
    });
    if(breadcrumbs.length > 0) {
        excludes = excludes.substring(0, excludes.length - 2);
    }
    excludes += ']';

    if(includes != '[]' || excludes != '[]') {
        regexes += '{ "regex" : "", ' + '"include" : ' + includes + ', "exclude" : ' + excludes + '}, ';
    }

    var regex_labels = $(".regex label");
    regex_labels.each(function() {
        var regex = $(this);
        regexes += '{ "regex" : "' + regex.text().trim() + '", ' + '"include" : [';
        if(regex.children('.must-have').length > 0) {
            regex.children('.must-have').each(function() {
                regexes += '"' + $(this).text() + '", ';
            });
            regexes = regexes.substring(0, regexes.length - 2);
        }
        regexes += '], "exclude" : [';

        if(regex.children('.not-have').length > 0) {
            regex.children('.not-have').each(function() {
                regexes += '"' + $(this).text() + '", ';
            });
            regexes = regexes.substring(0, regexes.length - 2);
        }
        regexes += ']}, ';
    });

    if(regex_labels.length > 0 || includes != '[]' || excludes != '[]') {
        regexes = regexes.substring(0, regexes.length - 2);
    }
    regexes += "]";

    socket.send("Synthesize Regexes: " + examples + '\n' + regexes);

    // set the synthesis progress bar
    percent = 0;
    $('#load').css('width', '0px');
    $('#load').addClass('progress-bar-striped active');
    if($('#load').hasClass('bg-danger')) {
        $('#load').removeClass('bg-danger');
    } else if($('#load').hasClass('bg-success')) {
        $('#load').removeClass('bg-success');
    }

    timerId = setInterval(function() {
        // increment progress bar
        percent += 1;
        $('#load').css('width', percent + '%');
        $('#load').html(percent + '%');

        if(percent == 96 && !isComplete) {
            // if not complete yet, pause at 96%
            percent -= 1;
            return;
        } 

        if (percent >= 100) {
          if(!isComplete || isTimeout == -1) {
            // there is a concurrent issue
            percent = 100;
            return;
          }
          
          clearInterval(timerId);
          $('#load').removeClass('progress-bar-striped active');

          if(isTimeout == 0) {   
            $('#load').addClass("bg-danger");   
            $('#load').html('synthesis timeout');   
          } else if (isTimeout == 1) {  
            $('#load').addClass("bg-success");  
            $('#load').html('synthesis complete');  

            // remove synthetic examples from the previous synthesis iteration  
            $("#synthetic tr").remove();    
          }
        }
    }, 900);

    // clean the previous synthesized regexes and generated examples (if any)
    $("#regex-container div.regex").remove();
    $("#synthetic tr").remove();
    // also remove the slider
    $('div#slider-container').empty();
}

function escapeHtml(unsafe) {
    return unsafe
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

function unescapeHtml(unsafe) {
    return unsafe
         .replace(/&amp;/g, "&")
         .replace(/&lt;/g, "<")
         .replace(/&gt;/g, ">")
         .replace(/&quot;/g, "\"")
         .replace(/&#039;/g, "'");
}

function escapeJson(unsafe) {
    return unsafe
        .replace(/\\/g, '\\\\')
        .replace(/"/g, '\\"');
}

function displayRegex(regexJson) {
    var regexArray = JSON.parse(regexJson);

    if(regexArray.length == 0) {
        isTimeout = 0;   
        percent = 100;
    } else {
        isTimeout = 1;
        percent = 100;
    }

    // set the signal for the progress bar
    isComplete = true;

    var count = 0;
    regexArray.forEach(function(regex) {
        $("#regex-container").append('<div class="form-check regex">' + 
            '<input class="form-check-input" type="checkbox" value="" id="regex' + count + '">' + 
            '<label class="form-check-label" for="regex' + count + '">' + 
            escapeHtml(regex) +
            '</label></div>');
        count++;         
    });
}

function removeAnnotation(id) {
    var button = $("button#"+id);
    var annotation = button.prev().text();
    var type;
    if(button.prev().hasClass('must-have')) {
        type = "must-have";
    } else {
        type = "not-have";
    }

    // remove the corresponding annotation in synthesized regexes
    var regexes = $(".regex label");
    regexes.each(function() {
        var regex = $(this);
        if(regex.text().includes(annotation)) {
            regex.children('.' + type).each(function() {
                if($(this).text() == annotation) {
                    $(this).replaceWith(escapeHtml($(this).text()));
                }
            });
        }
    });

    button.prev().remove();
    button.remove();
}

var sel_regex;
var synthetic_examples = {};
var isDisplayWildExamples = false;

function displaySimilarExamples() {
    isDisplayWildExamples = false;
    // check if we have generated the current selected regexes before
    var selected_regexes = getSelectedRegexes();
    if(selected_regexes != "") {
        var message = synthetic_examples[selected_regexes];
        if(message == null) {
            // generate examples for the selected regexes
            generateSyntheticExamples(selected_regexes);
        } else {
            // display the previously generated similar examples
            var similarExamples = message.split("\n")[0];
            displaySyntheticExamples(similarExamples, false);
        }
    }
}

function displayWildExamples() {
    isDisplayWildExamples = true;
     // check if we have generated the current selected regexes before
    var selected_regexes = getSelectedRegexes();
    if(selected_regexes != "") {
        var message = synthetic_examples[selected_regexes];
        if(message == null) {
            // generate examples for the selected regexes
            generateSyntheticExamples(selected_regexes);
        } else {
            // display the previously generated similar examples
            var wildExamples = message.split("\n")[1];
            displaySyntheticExamples(wildExamples, true);
        }
    }
}

function getSelectedRegexes() {
    // get selected regexes if any
    var regex_checkboxs = $(".regex input");
    var all_regexes = '[';
    var selected_regexes = '[';
    regex_checkboxs.each(function() {
        var checkbox = $(this);
        if(checkbox.is(':checked')) {
            selected_regexes += '"' + checkbox.next().text().trim() + '",'; 
        }
        all_regexes += '"' + checkbox.next().text().trim() + '",';
    });

    if(all_regexes.endsWith(",")) {
        all_regexes = all_regexes.substring(0, all_regexes.length - 1) + "]";   
    } else {
        alert("No regular expressions have been synthesized yet.");
        return "";
    }

    if(selected_regexes.endsWith(",")) {
        selected_regexes = selected_regexes.substring(0, selected_regexes.length - 1) + "]";
        sel_regex = JSON.parse(selected_regexes);
        return selected_regexes; 
    } else {
        // no regexes are selected
        alert("Please select one or more regular expressions first.");
        return "";
    }
}

function generateSyntheticExamples(regexes) {
    var rows = $("#examples tbody#task-" + cur_task + " tr");
    var examples = '[';
    rows.each(function() {
        var input = $(this).children('td').eq(0);
        var output = $(this).children('td').eq(1);
        if(output.text() == 'check') {
            examples += '"' + input.text().trim() + '",';
        } 
    });

    if(examples.endsWith(',')) {
        examples = examples.substring(0, examples.length - 1);
    } else {
        // no input examples
        alert("Please enter some examples first.");
        return;
    }

    examples += ']';

    socket.send("Generate Examples: " + examples + "\n" + regexes);
}

function displaySyntheticExamples(exampleJson, isWild) {
    // remove previous synthetic examples
    $("#synthetic tr").remove();
    $('div#slider-container').empty();

    var clusters = []; 
    var headers = [];
    // get the maximum of examples in a cluster
    var maxNum = 0;
    var cluster_counter = 0;
    
    // synthetic examples are now represented as a map in which key is the explanation of a cluster and value is a cluster of input-output examples
    var map = JSON.parse(exampleJson);
    for(var explanation in map) {
        headers[cluster_counter] = explanation;
        clusters[cluster_counter] = map[explanation];
        cluster_counter ++; 
    }

    clusters.forEach(function(cluster) {
        var count = 0;
        for(let example in cluster) {
            count++;
        }
        if(count > maxNum) {
            maxNum = count;
        }
    });

    if(maxNum == 0) {
        // no examples are generated
        if(isWild) {
            if(sel_regex.length == 1) {
                $('div#slider-container').append("<p>Oops, no examples are generated. Do you want to try clicking on 'Show me familiar examples' instead?</p>");
            } else {
                $('div#slider-container').append("<p>These selected regexes are logically the same. We cannot find examples that distinguish their behavior.</p>");
            }
        } else {
            if(sel_regex.length == 1) {
                $('div#slider-container').append("<p>Oops, no examples are generated. Do you want to try clicking on 'Show me corner cases' instead?</p>");
            } else {
                $('div#slider-container').append("<p>We cannot generate examples that distinguish these selected regexes based on the examples you give. "
                    + "Maybe these regexes are equivalent. Do you want to try clicking on 'Show me corner cases' to double check?</p>");
            }
        }
        return;
    }

    var num_to_display = maxNum > 5 ? 5 : maxNum;
    // generate the slider
    $('div#slider-container').append('<label for="exampleSlider">Number of Examples Per Cluster: </label><label id="sliderValue" style="margin-left:10px">' + num_to_display + '</label><br>');
    $('div#slider-container').append('<label style="margin:5px 10px 5px 10px;">0</label>' + 
            '<input type="range" class="custom-range" style="width: 80%; padding-top: 10px; margin:0px 10px 5px 10px;" id="exampleSlider" min="0" max="' + maxNum + '" step="1" value="' + num_to_display + '">' + 
            '<label id="maxSliderValue" style="margin:5px 10px 5px 10px;">' + maxNum + '</label>');

    // add a listener to the slider
    $('#exampleSlider').on('input', function(){
        // console.log(this.value);
        $('#sliderValue').text(this.value);

        var rows = $("#synthetic tr");

        var num_to_display = this.value;
        if(sel_regex.length > 1) {
            num_to_display ++;
        }

        var count = 0;
        rows.each(function(){
            var row = $(this);

            if(row.has('th').length == 1) {
                // new cluster
                count = 0;
            } else {
                var attr = row.attr('style');
                if(count < num_to_display) {
                    if (!(typeof attr == typeof undefined || attr == false)) {
                        row.removeAttr('style');
                    }
                } else {
                    if (typeof attr == typeof undefined || attr == false) {
                        row.attr('style', 'display:none');
                    }
                }

                count++;
            }
        });
    });

    if(sel_regex.length == 1) {
        // only need to show examples of one regex
        var i = 1;
        clusters.forEach(function(cluster) {
            // sort examples in the same cluster by length
            var arr = [];
            for(let example in cluster) {
                arr.push(example);
            }

            arr.sort(function(a, b){
                return a.length - b.length;
            });

            // try our best to show positive and negative examples next to each other after sorting
            var positives = [];
            var negatives = [];
            for(var j = 0; j < arr.length; j++) {
                var example = arr[j];
                if(cluster[example]) {
                    positives.push(example);
                } else {
                    negatives.push(example);
                }
            }
            // sort examples in array again to pair positive and negative examples
            var newArr = [];
            var len = positives.length > negatives.length ? negatives.length : positives.length; 
            var k = 0;
            for(; k < len; k++) {
                newArr.push(negatives[k]);
                newArr.push(positives[k]);
            }
            len = positives.length > negatives.length ? positives.length : negatives.length; 
            for(; k < len; k++) {
                // add the rest of examples
                if(len == positives.length) {
                    newArr.push(positives[k]);
                } else {
                    newArr.push(negatives[k]);
                }
            }

            arr = newArr;

            $("#synthetic tbody").append('<tr><th colspan="3" class="table-active" style="text-align: center;">Cluster ' + i + ': ' + headers[i-1] + '</th></tr>');
            
            var count = 0;
            for(var j = 0; j < arr.length; j++) {
                var example = arr[j];
                var display = ' style="display:none"';
                if(count < num_to_display) {
                    display = '';
                }

                var row;
                // if(isWild) {
                    // row = '<tr' + display + '><td>' + example + '</td>';
                    if(cluster[example]) {
                        row = '<tr' + display + '><td><span class="cluster2">' + example + '</span></td>';
                    } else {
                        // get the index of the failure-inducing character
                        if(example == ",0") {
                            // empty string
                            row = '<tr' + display + '><td><span class="cluster6"></span></td>'; 
                        } else {
                            var index = example.lastIndexOf(",");
                            var charIndex = parseInt(example.substring(index + 1));
                            var s1 = example.substring(0, charIndex);
                            var s2 = example.substring(charIndex, charIndex+1); 
                            var s3 = example.substring(charIndex+1, index);
                            row = '<tr' + display + '><td><span class="cluster2">' + escapeHtml(s1) + '</span><span class="cluster6">' + escapeHtml(s2) + '</span>' + escapeHtml(s3) + '</td>';
                        }
                    }
                // } else {
                //     var s1 = example.substring(0, i-1);
                //     var s2 = example.substring(i-1, i); 
                //     var s3 = example.substring(i);

                //     // row = '<tr' + display + '><td>' + s1 + '<span class="cluster' + classId + '">' + s2 + '</span>' + s3 + '</td>';
                //     if(cluster[example]) {
                //         // only show which character has been changed and use color to indicates the result
                //         // row = '<tr' + display + '><td>' + s1 + '<span class="cluster2">' + s2 + '</span>' + s3 + '</td>';
                //         // highlight each character based on the matching result
                //         row = '<tr' + display + '><td><span class="cluster2">' + example + '</span></td>';
                //     } else {
                //         row = '<tr' + display + '><td><span class="cluster2">' + s1 + '</span><span class="cluster6">' + s2 + '</span>' + s3 + '</td>';
                //     }
                // }

                var resultText;
                var resultClass;
                if(cluster[example] == true) {
                    resultText = 'check';
                    resultClass = 'match';
                } else {
                    resultText = 'close';
                    resultClass = 'unmatch';
                }

                $("#synthetic tbody").append(row +
                                      '<td class="icon"><i class="material-icons ' + resultClass + '">' + resultText + '</i></td>' + 
                                      '<td class="icon">' + 
                                            '<a class="add" title="Add" data-toggle="tooltip"><i class="material-icons">&#xE03B;</i></a>' + 
                                            '<a class="edit" title="Edit" data-toggle="tooltip"><i class="material-icons">&#xE254;</i></a>' + 
                                            '<a class="move" title="Add as a new example" data-toggle="tooltip"><i class="material-icons">add</i></a>' +
                                      '</td></tr>');
                count++;
            }
            i++;         
        });
    } else {
        // show distinguishing examples
        var i = 1;
        clusters.forEach(function(cluster) {
            $("#synthetic tbody").append('<tr><th colspan="' + (2 + sel_regex.length) + 
                    '" class="table-active" style="text-align: center;">Cluster ' + i + ': ' + headers[i-1] + '</th></tr>');
            var header = '<tr><td>Example</td>';
            for(j=0; j < sel_regex.length; j++) {
                header += '<td style="word-wrap: break-word">' + escapeHtml(sel_regex[j]) + '</td>';
            }
            header += '<td></td></tr>';

            $("#synthetic tbody").append(header);

            var count = 0; 
            for(let example in cluster) {
                var display = ' style="display:none"';
                if(count < num_to_display) {
                    display = '';
                }

                var row;
                // if(isWild) {
                //     row = '<tr' + display + '><td>' + example + '</td>';
                // } else {
                //     var s1 = example.substring(0, i-1);
                //     var s2 = example.substring(i-1, i); 
                //     var s3 = example.substring(i);

                //     row = '<tr' + display + '><td>' + s1 + '<span class="cluster' + classId + '">' + s2 + '</span>' + s3 + '</td>';
                // }

                row = '<tr' + display + '><td>' + escapeHtml(example) + '</td>';

                var results = cluster[example];
                for(j=0; j < results.length; j++) {
                    var resultText;
                    var resultClass;
                    if(results[j]== true) {
                        resultText = 'check';
                        resultClass = 'match';
                    } else {
                        resultText = 'close';
                        resultClass = 'unmatch';
                    }

                    row += '<td class="icon"><i class="material-icons ' + resultClass + '">' + resultText + '</i></td>';
                }

                row += '<td class="icon">' + 
                            '<a class="add" title="Add" data-toggle="tooltip"><i class="material-icons">&#xE03B;</i></a>' + 
                            '<a class="edit" title="Edit" data-toggle="tooltip"><i class="material-icons">&#xE254;</i></a>' + 
                            '<a class="move" title="Add as a new example" data-toggle="tooltip"><i class="material-icons">add</i></a>' +
                       '</td></tr>';

                $("#synthetic tbody").append(row);
                count++;
            }
            i++;         
        });
    }
}

function getOrdinalNum(i) {
    if(i == 1) {
        return "1st";
    } else if (i == 2) {
        return "2nd";
    } else if (i == 3) {
        return "3rd";
    } else {
        return i + "th";
    }
}

var generalizations = {};

function selectCharFamily() {
    if(typeof generalized_chars != 'undefined' && generalized_chars != null) {
        // get user selection
        $('div#charFamilies input').each(function() {
            var checkbox = $(this);
            if(checkbox.is(':checked')) {
                var id = checkbox.attr('id');
                generalizations[generalized_chars] = id;
                // uncheck the box otherwise it will still be checked the next time users open it
                checkbox.prop( "checked", false );
            }
        });
    }

    // dismiss the modal
    $('#charFamilyModal').modal('hide');

    // reset 
    generalized_chars = null;
}

function cancelCharFamily() {
    if(typeof generalized_chars != 'undefined' && generalized_chars != null) {
        // undo the highlight
        $('span.char-family').each(function() {
            var text = escapeHtml($(this).text());
            if(text == generalized_chars) {
                $(this).replaceWith($(this).text());
            }
        });
    } 

    // dismiss the modal
    $('#charFamilyModal').modal('hide');

    // reset 
    generalized_chars = null;
}

/* 
function showMore() {
    var rows = $("#synthetic tr");

    var flag = false;
    rows.each(function(){
        var row = $(this);
        if(row.has('th').length == 1) {
            // new cluster
            flag = true;
        } else {
            if(flag) {
                var attr = row.attr('style');
                if (typeof attr !== typeof undefined && attr !== false) {
                    row.removeAttr('style');
                    flag = false;
                }
            }
        }
    });
}

function showLess() {
    var rows = $("#synthetic tr");

    var flag = false;
    var hasHidden = false;
    rows.each(function(){
        var row = $(this);

        if(row.has('th').length == 1) {
            // new cluster
            flag = true;

            if(!hasHidden) {
                // no hidden examples in the previous cluster
                var prev_tr = row.prev('tr');
                if(prev_tr.length != 0 && prev_tr.has('th').length == 0 && prev_tr.has('td.icon').length > 0) {
                    // make sure it is not a cluster header
                    prev_tr.attr('style', 'display:none');
                }
            }

            // reset
            hasHidden = false;
        } else {
            if(flag) {
                var attr = row.attr('style');
                if (typeof attr !== typeof undefined && attr !== false) {
                    hasHidden = true;
                    // find the previous visible sibling
                    var prev_tr = row.prev('tr');
                    if(prev_tr.length != 0 && prev_tr.has('th').length == 0 && prev_tr.has('td.icon').length > 0) {
                        // make sure it is not a cluster header
                        prev_tr.attr('style', 'display:none');
                        flag = false;
                    }
                }
            }
        }
    });
}

var allDisplayed = false;
function showAll() {
    var rows = $("#synthetic tr");

    var num_to_display = 5;
    if(sel_regex.length > 1) {
        num_to_display ++;
    }

    if(!allDisplayed) {
        // show all hidden examples
        rows.each(function() {
            var row = $(this);
            if(row.has('th').length == 0) {
                // not a cluster header
                var attr = row.attr('style');
                if (typeof attr !== typeof undefined && attr !== false) {
                    row.removeAttr('style');
                }
            }
        });

        allDisplayed = true;
    } else {
        // return to the default display
        var count = 0;
        rows.each(function(){
            var row = $(this);

            if(row.has('th').length == 1) {
                // new cluster
                count = 0;
            } else {
                if(count < num_to_display) {                       
                    count++;
                } else {
                    var attr = row.attr('style');
                    if (typeof attr == typeof undefined || attr == false) {
                        row.attr('style', 'display:none');
                    }
                }
            }
        });

        allDisplayed = false;
    }
}
*/
