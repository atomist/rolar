'use strict';

const React = require('react');
const ReactDOM = require('react-dom');
const _ = require('lodash');

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            logs: []
        };
    }

    componentDidMount() {
        let auth = "";
        if (this.props.auth) {
            auth = `&auth=${this.props.auth}`
        }
        const url = '/api/reactive/logs/' + this.props.path + '?after=' + this.props.after + auth;
        const source = new EventSource(url);
        source.addEventListener("message", logResultsEvent => {
            const logResults = JSON.parse(logResultsEvent.data);
            if (logResults.lastKey && logResults.lastKey.closed) {
                const specifiedPath = this.props.path.split("/");
                if(_.isEqual(specifiedPath, logResults.lastKey.path)) {
                    source.close();
                }
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
        const logs = this.props.logs.map(log =>
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

const root = document.getElementById('react');
ReactDOM.render(<App {...(root.dataset)} />, root);
