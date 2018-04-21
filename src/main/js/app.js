'use strict';

const React = require('react');
const ReactDOM = require('react-dom');

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            logs: []
        };
    }

    componentDidMount() {
        var url = '/api/reactive/logs/' + this.props.path + '?after=' + this.props.after;
        var source = new EventSource(url);
        source.addEventListener("message", logResultsEvent => {
            var logResults = JSON.parse(logResultsEvent.data);
            if (logResults.lastKey && logResults.lastKey.closed) {
                source.close();
            }
            this.state.logs.push(...logResults.logs);
            this.setState({
                logs: this.state.logs
            });
        });
    }

    render() {
        return (
            <LogList logs={this.state.logs}/>
        )
    }
}

class LogList extends React.Component{
    render() {
        var logs = this.props.logs.map(log =>
            <Log key={log.timestamp + log.message} log={log}/>
        );
        return (
            <table>
                <tbody>
                    {logs}
                </tbody>
            </table>
        )
    }
}
class Log extends React.Component{
    render() {
        return (
            <tr>
                <td>{this.props.log.timestamp}</td>
                <td>{this.props.log.level}</td>
                <td>{this.props.log.message}</td>
            </tr>
        )
    }
}

var root = document.getElementById('react');
ReactDOM.render(<App {...(root.dataset)} />, root);
