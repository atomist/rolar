'use strict';

const React = require('react');
const ReactDOM = require('react-dom');
const client = require('./client');

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {logs: []};
    }

    componentDidMount() {
        client({method: 'GET', path: '/api/logs/staging/Clays-MacBook-Pro.local'}).done(response => {
            this.setState({logs: response.entity});
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

ReactDOM.render(
    <App />,
    document.getElementById('react')
)
